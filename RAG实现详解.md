# RAG (检索增强生成) 完整实现详解

> 本文档详细记录了在 Nexus 电商 AI Agent 项目中实现 RAG 的全过程，包括每一步的技术选型、代码实现和工作原理。

---

## 目录

1. [文档解析与清洗](#步骤1文档解析与清洗)
2. [文本分块](#步骤2文本分块)
3. [文本向量化](#步骤3文本向量化)
4. [构建向量索引并存储](#步骤4构建向量索引并存储)
5. [用户问题向量化](#步骤5用户问题向量化)
6. [语义检索与重排](#步骤6语义检索与重排)
7. [Prompt 组装与大模型生成](#步骤7prompt-组装与大模型生成)

---

## 架构概览

RAG 分为两个阶段：

**阶段一：离线数据注入（步骤 1-4）**

```
原始文档 (TXT)
    ↓
[步骤1] 解析与清洗
    ↓
干净的纯文本
    ↓
[步骤2] 文本分块（300 token/块，50 token 重叠）
    ↓
文本块数组 [chunk1, chunk2, ...]
    ↓
[步骤3] 向量化（阿里云百炼 text-embedding-v3）
    ↓
向量数组（每个向量 1024 维浮点数）
    ↓
[步骤4] 存入 pgvector，自动建立 HNSW 索引
```

**阶段二：在线检索生成（步骤 5-7）**

```
用户问题："耳机保修多久？"
    ↓
[步骤5] 用同一个 Embedding 模型将问题向量化
    ↓
查询向量（1024 维）
    ↓
[步骤6] 向量检索 Top-5 → Rerank 重排 → 保留 Top-3
    ↓
最相关的 3 段知识库文本
    ↓
[步骤7] 拼接进 System Prompt → 发给 DeepSeek
    ↓
AI 基于资料回答，不知道就说不知道
```

---

## 步骤1：文档解析与清洗

### 目标

将原始文档转换为干净的纯文本，剥离无效内容，只保留核心语义。

### 技术栈

| 技术 | 用途 |
|------|------|
| Spring AI `TextReader` | 读取 TXT/Markdown 文件 |
| Java 正则表达式 | 清洗：去空行、压缩空格、统一换行符 |
| `PathMatchingResourcePatternResolver` | 批量扫描 classpath 下的文档目录 |

### 代码实现

文件位置：`agent-demo/src/main/java/com/powernode/agentdemo/service/KnowledgeService.java`

```java
public String importDocuments() throws IOException {
    // 扫描 resources/documents/ 目录下的所有 txt 文件
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource[] resources = resolver.getResources("classpath:documents/*.txt");

    List<Document> allChunks = new ArrayList<>();
    for (Resource resource : resources) {

        // --- 步骤 1.1：解析文档 ---
        TextReader reader = new TextReader(resource);
        List<Document> rawDocs = reader.get();
        // TextReader 读出的是 Spring AI 的 Document 对象
        // 包含两个字段：content（文本内容）、metadata（元数据，如文件名）

        // --- 步骤 1.2：清洗文本 ---
        List<Document> cleanedDocs = rawDocs.stream()
                .map(doc -> {
                    String cleaned = doc.getText()
                            .replaceAll("\\r\\n", "\n")     // Windows换行 → Unix换行
                            .replaceAll("\\n{3,}", "\n\n")  // 3个以上空行 → 2个
                            .replaceAll("[ \\t]+", " ")     // 连续空格/Tab → 单个空格
                            .trim();                        // 去掉首尾空白
                    return new Document(cleaned, doc.getMetadata());
                })
                .toList();

        // 后续分块...
        List<Document> chunks = splitter.apply(cleanedDocs);
        allChunks.addAll(chunks);
    }

    vectorStore.add(allChunks);
    return "成功导入 " + resources.length + " 篇文档，切分为 " + allChunks.size() + " 个块";
}
```

### 清洗规则详解

| 正则表达式 | 替换为 | 作用 |
|-----------|--------|------|
| `\\r\\n` | `\n` | 统一换行符，Windows 文件是 CRLF，Linux/Mac 是 LF |
| `\\n{3,}` | `\n\n` | 压缩连续空行，3个以上空行统一变成2个 |
| `[ \\t]+` | ` ` | 压缩连续空格和Tab，"hello····world" → "hello world" |
| `.trim()` | — | 去掉整段文字首尾的空白字符 |

### 支持格式现状

| 格式 | 支持 | 方案 |
|------|------|------|
| TXT | ✅ 已实现 | Spring AI `TextReader` |
| Markdown | ✅ 可用 | 同 TXT，Markdown 本质是纯文本 |
| PDF | ❌ 未实现 | 需引入 Apache Tika：`new Tika().parseToString(file)` |
| Word(.docx) | ❌ 未实现 | 需引入 Apache POI |
| HTML | ❌ 未实现 | 需引入 Jsoup：`Jsoup.parse(html).text()` |

---

## 步骤2：文本分块

### 目标

将长文档切成小块（Chunk），每块约 300 token，块间保留 50 token 重叠，防止核心信息在切分点被截断。

### 为什么要分块？

1. **上下文窗口有限**：大模型每次能接受的文本有上限（如 8K token），整篇文章塞不进去
2. **检索精度问题**：整篇文章的向量是所有内容的"平均值"，无法精确匹配具体问题
3. **降低成本**：只把相关片段发给模型，比发整篇文档节省大量 token 费用

### 技术栈

| 技术 | 说明 |
|------|------|
| Spring AI `TokenTextSplitter` | 按 token 数量分块（不是按字符数，更精准） |
| **chunkSize = 300** | 每块目标大小为 300 token |
| **overlap = 50** | 块间重叠 50 token（约 16.7%） |
| **minChunkSize = 50** | 小于 50 token 的碎片块丢弃或合并 |
| **maxChunks = 10000** | 单次最多切 10000 块 |

### 代码实现

```java
// 初始化分块器（项目启动时创建一次，复用）
private final TokenTextSplitter splitter = new TokenTextSplitter(
    300,    // chunkSize：每块目标大小（token数）
    50,     // overlap：相邻块的重叠大小（token数）
    50,     // minChunkSize：低于此值的块会被丢弃
    10000,  // maxChunks：单篇文档最多切成多少块
    true    // keepSeparator：切分时保留换行符等分隔符
);

// 执行分块
List<Document> chunks = splitter.apply(cleanedDocs);
// cleanedDocs 是清洗后的 Document 列表
// chunks 是切分后的小块列表，每块仍是 Document 对象
```

### 分块示意图

```
原始文本（900 token）：
|===========================================|

分块结果（300 token/块，50 token 重叠）：

|------- Chunk1(300) -------|
                  |------- Chunk2(300) -------|
                                  |------- Chunk3(300) -------|
                  |<50>|          |<50>|
                  重叠区           重叠区
```

### 重叠的作用

没有重叠时，关键信息可能被切断：
```
Chunk1 末尾: "...无线蓝牙耳机 P001 的保修期为"
Chunk2 开头: "一年，期间免费维修..."
→ 两块单独检索时都拿不到完整信息
```

有重叠时：
```
Chunk1: "...无线蓝牙耳机 P001 的保修期为一年，..."    ← 包含完整信息
Chunk2: "保修期为一年，期间免费维修换件..."           ← 也包含完整信息
→ 无论命中哪块，都能拿到完整答案
```

### 参数选择建议

| 场景 | chunkSize | overlap | 理由 |
|------|-----------|---------|------|
| 产品说明、FAQ | 300-500 | 50-100 | 信息密度中等，本项目选择 |
| 法律合同、技术文档 | 500-800 | 100-200 | 长句多，需要更多上下文 |
| 新闻、短文 | 100-200 | 20-50 | 信息短小精悍 |

---

## 步骤3：文本向量化

### 目标

将每个文本块转换为 1024 维的浮点数向量，语义相似的文本在向量空间中距离更近。

### 技术栈

| 技术 | 规格 |
|------|------|
| **Embedding 模型** | 阿里云百炼 `text-embedding-v3` |
| **向量维度** | 1024 维 |
| **API 协议** | OpenAI 兼容格式 |
| **Spring AI 接口** | `EmbeddingModel`（框架自动调用，开发者无需手写） |

### 配置

文件位置：`nexus-backend/src/main/resources/application.yml`

```yaml
spring:
  ai:
    openai:
      embedding:
        api-key: ${ALIYUN_API_KEY}
        base-url: https://dashscope.aliyuncs.com/compatible-mode
        options:
          model: text-embedding-v3   # 输出 1024 维向量
```

### 代码实现

向量化由 Spring AI 在 `vectorStore.add()` 内部自动完成，开发者无需手动调用：

```java
// 这一行代码背后，Spring AI 自动执行了向量化：
vectorStore.add(allChunks);

// 框架内部等价于：
for (Document chunk : allChunks) {
    // 1. 调用阿里云百炼 API，将文本转为向量
    float[] vector = embeddingModel.embed(chunk.getText());
    // vector = [0.023, -0.145, 0.678, ..., -0.312]  共 1024 个数字

    // 2. 将文本+向量存入 pgvector
    jdbcTemplate.update(
        "INSERT INTO vector_store (content, embedding, metadata) VALUES (?, ?, ?)",
        chunk.getText(), vector, chunk.getMetadata()
    );
}
```

### 向量化原理

**输入（文本块）**：
```
"无线蓝牙耳机 P001 保修期为一年，在保修期内如因产品质量问题损坏，可免费维修或更换。"
```

**输出（1024维向量）**：
```
[0.023, -0.145, 0.678, 0.234, -0.891, 0.012, ..., -0.312]
       ↑ 共 1024 个浮点数，每个数字代表文本在某个语义维度上的特征值
```

**语义距离示例**：
```
"耳机保修多久"    → [0.031, -0.152, 0.701, ...]
"耳机保修一年"    → [0.028, -0.148, 0.695, ...]  ← 距离很近（语义相似）
"手机外壳颜色"    → [0.891,  0.234, -0.456, ...] ← 距离很远（语义不同）
```

### 模型对比

| 模型 | 维度 | 中文效果 | 费用 |
|------|------|---------|------|
| 阿里云百炼 text-embedding-v3（本项目） | 1024 | 优秀 | ¥0.0005/千token |
| OpenAI text-embedding-3-small | 1536 | 优秀 | $0.02/百万token |
| OpenAI text-embedding-ada-002 | 1536 | 良好 | $0.1/百万token |
| BGE-large-zh（本地部署） | 1024 | 优秀 | 免费 |

---

## 步骤4：构建向量索引并存储

### 目标

将文本块、向量、元数据持久化到向量数据库，并建立 HNSW 索引，支持毫秒级的相似度检索。

### 技术栈

| 技术 | 角色 |
|------|------|
| **PostgreSQL 16** | 底层关系数据库，提供事务、持久化 |
| **pgvector 扩展** | PostgreSQL 插件，增加 `vector` 数据类型和相似度搜索 |
| **HNSW 索引** | 向量近似最近邻搜索算法，毫秒级检索 |
| **Spring AI PgVectorStore** | Java 封装层，自动建表、建索引、增删查 |
| **Docker** | 容器化部署 pgvector |

### 部署步骤

**第一步：Docker 启动 pgvector**
```bash
docker run -d \
  --name pgvector \
  -e POSTGRES_PASSWORD=123456 \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

**第二步：创建数据库**
```sql
CREATE DATABASE nexus_agent;
```

**第三步：Spring AI 自动建表**（首次启动时自动执行）

配置 `initializeSchema(true)` 后，Spring AI 会自动执行：
```sql
-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 创建存储表
CREATE TABLE IF NOT EXISTS vector_store (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content   TEXT,                   -- 原始文本块
    metadata  JSONB,                  -- 元数据（文件名、分类等）
    embedding vector(1024)            -- 1024维向量
);

-- 创建 HNSW 索引（加速检索）
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
    ON vector_store
    USING hnsw (embedding vector_cosine_ops);
```

### Java 配置代码

文件位置：`nexus-backend/src/main/java/com/nexus/config/VectorStoreConfig.java`

```java
@Configuration
public class VectorStoreConfig {

    // pgvector 专用数据源（与 MySQL 业务数据源完全隔离）
    @Bean("pgVectorDataSource")
    public DataSource vectorDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl("jdbc:postgresql://localhost:5432/nexus_agent");
        ds.setUsername("postgres");
        ds.setPassword("123456");
        return ds;
    }

    @Bean
    public VectorStore vectorStore(
            @Qualifier("pgVectorDataSource") DataSource vectorDataSource,
            EmbeddingModel embeddingModel) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(vectorDataSource);
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1024)        // 必须与 Embedding 模型输出维度一致
                .initializeSchema(true)  // 自动建表建索引
                .build();
    }
}
```

### HNSW 索引原理

HNSW（Hierarchical Navigable Small World，分层导航小世界）是目前最主流的向量索引算法：

```
第2层（稀疏，长跳）:   [1] ---------> [50] ---------> [100]

第1层（中等）:         [1] -> [20] -> [50] -> [80] -> [100]

第0层（密集，全量）:   [1][5][10][15][20]...[50]...[80][85]...[100]

检索过程（查找与问题最相似的向量）：
  1. 从第2层入口节点出发，快速跳到大致区域（覆盖整个空间）
  2. 下降到第1层，缩小搜索范围
  3. 在第0层精确遍历邻居节点，找到最近的 Top-K
```

| 索引方式 | 查询时间 | 精度 | 内存 |
|---------|---------|------|------|
| HNSW（本项目） | 1-10ms | 99%+ | 较高 |
| IVFFlat | 10-100ms | 95% | 中等 |
| 无索引（暴力搜索） | 秒级 | 100% | 低 |

---

## 步骤5：用户问题向量化

### 目标

用户每次提问时，实时将问题文字转换为 1024 维向量，用于在 pgvector 中检索语义最相近的文本块。

### 技术栈

| 技术 | 说明 |
|------|------|
| 同步骤3的 Embedding 模型 | **必须**与存储时用同一个模型，维度和语义空间才一致 |
| 实时调用 | 每次用户发消息都会调用一次 API，耗时约 100-300ms |
| Spring AI `VectorStore.similaritySearch()` | 内部自动完成问题向量化，开发者无需手写 |

### 代码实现

文件位置：`nexus-backend/src/main/java/com/nexus/agent/core/SpringAIAgentExecutor.java`

```java
private String searchKnowledge(String query) {
    var results = vectorStore.similaritySearch(
        SearchRequest.builder()
                .query(query)              // 用户原始问题文字
                .topK(5)
                .similarityThreshold(0.4)
                .build()
    );
    // Spring AI 在 similaritySearch 内部自动执行：
    // 1. float[] queryVector = embeddingModel.embed(query);
    //    → 调用阿里云百炼 API，将问题转为 1024 维向量
    // 2. 执行 pgvector 相似度查询：
    //    SELECT * FROM vector_store
    //    ORDER BY embedding <=> queryVector  -- <=> 是余弦距离算子
    //    WHERE 余弦距离 < 阈值
    //    LIMIT 5
}
```

### 为什么必须用同一个 Embedding 模型？

```
存储时用 text-embedding-v3 得到向量：
  "耳机保修一年" → [0.028, -0.148, 0.695, ...]（text-embedding-v3 的语义空间）

检索时用不同模型 text-ada-002：
  "保修多久"     → [0.521,  0.034, -0.123, ...]（ada-002 的语义空间，完全不同！）

→ 两个向量在不同空间，距离计算没有意义，找不到相关内容
```

### 相似度计算：余弦相似度

pgvector 使用余弦相似度（`<=>` 操作符）计算两个向量的距离：

```
余弦相似度 = (A · B) / (||A|| × ||B||)

值域：0 到 1（余弦距离 = 1 - 余弦相似度）
  距离 = 0    → 完全相同
  距离 = 0.2  → 高度相关（相似度 0.8）
  距离 = 0.6  → 低度相关（相似度 0.4）
  距离 = 1.0  → 完全无关
```

`.similarityThreshold(0.4)` 表示过滤掉相似度低于 0.4 的结果（即距离大于 0.6）。

---

## 步骤6：语义检索与重排

### 目标

从向量数据库召回 Top-5 候选文本块，再通过 Rerank 模型精准打分，筛选出真正相关的 Top-3，过滤掉"语义相似但实际无关"的噪音。

### 6.1 向量检索

#### 技术栈

| 技术 | 配置 |
|------|------|
| pgvector `<=>` 余弦距离检索 | HNSW 索引加速，毫秒级完成 |
| topK = 5 | 多召回几个，给 Rerank 留有筛选空间 |
| similarityThreshold = 0.4 | 过滤掉明显不相关的结果 |

#### 代码

```java
var results = vectorStore.similaritySearch(
    SearchRequest.builder()
            .query(query)
            .topK(5)
            .similarityThreshold(0.4)
            .build()
);
```

### 6.2 Rerank 重排

#### 为什么向量检索还不够？

向量相似度是"语义近似程度"，但不等于"回答问题的相关程度"：

```
问题："耳机保修多久？"

向量检索结果（按相似度排序）：
  排名1（相似度0.82）: "耳机产品说明：含保修期信息"  ← 真正有用
  排名2（相似度0.79）: "耳机使用教程：如何配对连接"  ← 语义相近但无关
  排名3（相似度0.76）: "手机保修政策：手机1年保修"   ← 保修相关但不是耳机
  排名4（相似度0.71）: "耳机常见问题：连接故障处理"  ← 耳机相关但无关保修
  排名5（相似度0.68）: "平台售后通用流程"            ← 笼统，价值低

问题：把这5段都塞进 Prompt，会干扰模型判断，让它产生错误联想
```

Rerank 模型专门训练来判断"文本与问题的相关度"，能精准区分上面的差异。

#### 技术栈

| 技术 | 规格 |
|------|------|
| Rerank 模型 | 阿里云百炼 `gte-rerank` |
| 输入 | 用户问题 + 5个候选文本 |
| 输出 | 每个候选的 `relevance_score`（0-1分） |
| 过滤阈值 | score < 0.3 的候选直接丢弃 |
| 最终保留 | Top-3 |

#### 代码实现

文件位置：`nexus-backend/src/main/java/com/nexus/agent/service/RerankService.java`

```java
@Slf4j
@Component
public class RerankService {

    private static final String RERANK_URL =
            "https://dashscope.aliyuncs.com/compatible-mode/v1/rerank";

    @Value("${spring.ai.openai.embedding.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<String> rerank(String query, List<String> candidates, int topN) {
        if (candidates.isEmpty()) return candidates;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);  // 用阿里云百炼的 API Key

            // 构造请求体
            Map<String, Object> body = Map.of(
                    "model", "gte-rerank",
                    "query", query,              // 用户问题
                    "documents", candidates,     // Top-5 候选文本列表
                    "top_n", Math.min(topN, candidates.size()),
                    "return_documents", false    // 只返回排名，不重复返回文本
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    RERANK_URL, new HttpEntity<>(body, headers), Map.class);

            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) response.getBody().get("results");

            // 过滤低分 + 按 relevance_score 降序排列（API 已帮我们排好）
            return results.stream()
                    .filter(r -> {
                        Double score = (Double) r.get("relevance_score");
                        return score != null && score > 0.3;  // 低于0.3的丢弃
                    })
                    .map(r -> {
                        Integer index = (Integer) r.get("index");
                        return candidates.get(index);  // 通过索引取回原始文本
                    })
                    .toList();

        } catch (Exception e) {
            // Rerank 失败时降级：直接返回向量检索的原始结果
            log.warn("Rerank 调用失败，降级使用原始顺序: {}", e.getMessage());
            return candidates.subList(0, Math.min(topN, candidates.size()));
        }
    }
}
```

#### 完整检索流程

文件位置：`SpringAIAgentExecutor.java`

```java
private String searchKnowledge(String query) {
    try {
        // 第一步：向量检索，召回 Top-5 候选
        var results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(5)
                        .similarityThreshold(0.4)
                        .build()
        );
        if (results == null || results.isEmpty()) return "";

        // 第二步：提取候选文本列表
        List<String> candidates = results.stream()
                .map(doc -> doc.getText())
                .toList();

        // 第三步：Rerank 重排，从5个里精选3个
        List<String> reranked = rerankService.rerank(query, candidates, 3);

        if (reranked.isEmpty()) return "";

        // 第四步：将3段文本用双换行拼接
        return String.join("\n\n", reranked);

    } catch (Exception e) {
        log.warn("知识库检索失败，跳过 RAG: {}", e.getMessage());
        return "";
    }
}
```

#### Rerank 前后对比

**问题：** "耳机保修多久？"

| 阶段 | 排名 | 分数 | 内容摘要 | 是否保留 |
|------|------|------|---------|---------|
| 向量检索后 | 1 | 相似度 0.82 | 耳机产品说明（含保修1年） | ✅ |
| 向量检索后 | 2 | 相似度 0.79 | 耳机使用教程（无保修内容） | ❌ 被Rerank过滤 |
| 向量检索后 | 3 | 相似度 0.76 | 手机保修政策（不是耳机） | ❌ 被Rerank过滤 |
| 向量检索后 | 4 | 相似度 0.71 | 耳机常见问题（连接故障） | ❌ 被Rerank过滤 |
| 向量检索后 | 5 | 相似度 0.68 | 平台售后通用流程 | ⚠️ 视分数决定 |
| **Rerank后** | 1 | **相关度 0.94** | 耳机产品说明（含保修1年） | ✅ 保留 |
| **Rerank后** | 2 | **相关度 0.61** | 平台售后通用流程 | ✅ 保留 |
| （其余全被过滤） | — | < 0.3 | — | ❌ |

---

## 步骤7：Prompt 组装与大模型生成

### 目标

将 Rerank 筛选出的知识库文本作为"参考资料"注入 System Prompt，约束大模型必须基于资料回答，不知道的明确说不知道，不编造内容。

### 7.1 System Prompt 结构

**三层结构**（每次请求都重新组装）：

```
System Prompt = [角色定义 + 职责 + 禁止事项]
              + [当前用户信息（可选）]
              + [知识库参考资料（可选，有检索结果才有）]
```

### 7.2 角色定义层

文件位置：`nexus-backend/src/main/java/com/nexus/agent/prompts/PromptRegistry.java`

核心内容：
```
你是 Nexus 电商平台的智能客服助手。

## 职责
- 回答商品相关问题（价格、规格、库存等）
- 解释订单状态和物流信息
- 处理用户投诉和问题

## 工具使用规范
- 查询商品信息使用 queryProduct 工具
- 查询订单状态使用 queryOrder 工具
- 优先使用工具获取准确信息，不要猜测

## 禁止事项
- 不要编造商品信息或订单信息
- 知识库资料之外的专业问题，必须明确说"暂无相关资料"，不得编造
```

### 7.3 知识库注入层（关键）

文件位置：`SpringAIAgentExecutor.java` 的 `buildSystemPrompt()` 方法

**有检索结果时**：

```java
if (!knowledge.isEmpty()) {
    basePrompt += "\n\n## 知识库参考资料\n"
            + "以下是与用户问题相关的资料，回答时必须遵守以下规则：\n"
            + "1. 优先且严格基于以下资料回答，不得添加资料中没有的信息\n"
            + "2. 如果资料中没有用户问题的答案，直接回复"抱歉，这个问题我暂时没有相关资料，建议您联系人工客服"\n"
            + "3. 不要用自身训练知识补充或猜测资料之外的内容\n"
            + "---------------------\n"
            + knowledge        // 这里是 Rerank 后的 Top-3 文本块
            + "\n---------------------";
}
```

**没有检索结果时**：

```java
else {
    basePrompt += "\n\n## 注意\n当前没有找到与问题相关的知识库资料。"
            + "如果问题涉及具体的产品规格、政策细节等专业内容，"
            + "请回复"抱歉，我暂时没有这方面的资料"，不要猜测。";
}
```

### 7.4 最终 Prompt 示例

用户问"耳机保修多久？"时，实际发给 DeepSeek 的完整 System Prompt：

```
你是 Nexus 电商平台的智能客服助手。

## 职责
- 回答商品相关问题（价格、规格、库存等）
...

## 禁止事项
- 不要编造商品信息
- 知识库资料之外的专业问题，必须明确说"暂无相关资料"
...

## 知识库参考资料
以下是与用户问题相关的资料，回答时必须遵守以下规则：
1. 优先且严格基于以下资料回答，不得添加资料中没有的信息
2. 如果资料中没有用户问题的答案，直接回复"抱歉，这个问题我暂时没有相关资料，建议您联系人工客服"
3. 不要用自身训练知识补充或猜测资料之外的内容
---------------------
无线蓝牙耳机（P001）保修期为一年。在保修期内，如因产品质量问题损坏，
可凭购买凭证到任意授权维修点免费维修或更换同款产品...

平台售后服务流程：提交申请→客服审核（1个工作日）→安排处理...
---------------------
```

然后用户问题作为 User Message 发送：
```
用户：耳机保修多久？
```

DeepSeek 收到完整上下文后，**只能基于资料回答**：
```
AI：根据产品说明，无线蓝牙耳机（P001）的保修期为**一年**。
    在保修期内，如因质量问题损坏，凭购买凭证可到授权维修点免费维修或更换同款产品。
```

### 7.5 "不知道就说不知道"效果对比

**用户问一个知识库中没有的问题**："耳机防水等级是多少？"

**优化前（无约束）**：
```
AI：该耳机防水等级为 IPX5，可以防溅水...
    （完全编造，知识库里没有这个信息）
```

**优化后（有约束）**：
```
AI：抱歉，这个问题我暂时没有相关资料，建议您联系人工客服获取准确信息。
    （诚实告知，不编造）
```

---

## 总结

| 步骤 | 执行时机 | 技术 | 关键参数 |
|------|---------|------|---------|
| 1. 解析与清洗 | 一次性离线 | Spring AI TextReader + 正则 | — |
| 2. 文本分块 | 一次性离线 | Spring AI TokenTextSplitter | 300 token，50 重叠 |
| 3. 向量化（存储） | 一次性离线 | 阿里云百炼 text-embedding-v3 | 1024 维 |
| 4. 建索引存储 | 一次性离线 | pgvector + HNSW | 自动建表 |
| 5. 向量化（查询） | 每次请求 | 同上，自动调用 | — |
| 6. 检索 + Rerank | 每次请求 | pgvector + gte-rerank | Top-5 → Top-3 |
| 7. Prompt 组装 | 每次请求 | 字符串拼接 + DeepSeek | 严格约束不编造 |
