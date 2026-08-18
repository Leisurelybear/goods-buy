# Import Data Converter Skill — 设计

日期：2026-08-18

## 目标

构建一个项目级 skill（`.claude/skills/import-data-converter/SKILL.md`），使用户可以指示 AI 把已有的各种格式记录（CSV / Excel / WPS 等）转换为 **App 可直接导入的 ZIP 备份包**，一次完成数据迁移并导入。

## 背景

App 只支持导入 **ZIP 备份包**（根目录 `manifest.json` + 可选 `images/`）。Excel 仅能通过仓库根目录 `convert_excel.py` 转成 `manifest.json` 后打包导入。本 skill 自包含，不依赖 `convert_excel.py`：AI 直接读取源文件、清洗、生成 `manifest.json`，并用 PowerShell `Compress-Archive` 打包。

## 目标格式（manifest.json 规范）

顶层结构：

```json
{
  "version": 1,
  "timestamp": 1786545728290,
  "collectibles": [ ... ]
}
```

每条记录字段（与 `BackupManager.kt:28-56` 一致）：

| 字段 | 类型 | 必填 | 说明 | 默认/约束 |
|---|---|---|---|---|
| `name` | String | 是 | 名称 | 非空 |
| `category` | String | 否 | 种类（手办/模型） | `""` |
| `type` | String | 否 | 类型（官方等） | `"官方"` |
| `ipName` | String | 否 | IP/系列名 | `""` |
| `seriesName` | String | 否 | 制品系列 | `""` |
| `characterTag` | String | 否 | 角色/标签 | `""` |
| `remark` | String | 否 | 备注 | `""` |
| `purchaseChannel` | String | 否 | 购买渠道 | `""` |
| `purchaseShop` | String | 否 | 购买店铺 | `""` |
| `purchaseDate` | Long | 否 | 购买日期 epoch 毫秒 | 现在 |
| `purchasePrice` | Double | 否 | 购买价格 | ≥0 |
| `purchaseQuantity` | Int | 否 | 购买数量 | >0 整数，默认 1 |
| `purchaseShipping` | Double | 否 | 购买运费 | ≥0 |
| `expectedPrice` | Double | 否 | 预期售价 | =sellPrice 或 0 |
| `sellPrice` | Double? | 否 | 售出价格 | SOLD 时必填 |
| `sellQuantity` | Int? | 否 | 售出数量 | SOLD 时必填，≤购买数量 |
| `sellShipping` | Double? | 否 | 售出运费 | ≥0 |
| `isFreeShipping` | Boolean | 否 | 包邮 | false |
| `sellDate` | Long? | 否 | 售出日期 epoch 毫秒 | null |
| `buyerInfo` | String? | 否 | 买家信息 | null |
| `sellRemark` | String? | 否 | 售出备注 | null |
| `status` | String | 否 | 状态英文枚举 | `OWNED` |
| `storageStatus` | String | 否 | 仓储英文枚举 | `IN_STOCK` |
| `imageFilenames` | List\<String\> | 否 | ZIP `images/` 文件名 | `[]` |
| `createdAt` | Long | 否 | 创建时间 | 现在 |
| `updatedAt` | Long | 否 | 更新时间 | 现在 |

枚举值（非法值在 App 内静默回退默认，skill 应提示用户）：

- `status`：`PENDING_TAIL`(待补尾款) `PENDING_SHIPPING_FEE`(待补邮) `PENDING_SEND`(待发货) `IN_TRANSIT_ORDER`(运输中) `OWNED`(已拥有) `HESITATING_SELL`(犹豫出售) `LISTED`(已挂出) `SOLD`(已售出) `GIFT`(赠品/付邮送) `LOST`(遗失/损坏)
- `storageStatus`：`IN_STOCK`(现货) `IN_TRANSIT`(在途) `GROUP_STORAGE`(团长囤货) `AGENT_STORAGE`(代购处囤货)

去重指纹（App 导入时用，skill 仅作重复提醒）：SHA-1 of `name, ipName, seriesName, characterTag, purchaseChannel, purchaseShop, remark, purchasePrice, purchaseQuantity, purchaseShipping, purchaseDate, status, storageStatus, sellPrice, sellDate, category, type`。

## 交互式流程（核心）

Skill 不假设"一个文件 = 一张表 = 每行一条记录"。先摸清结构，再选策略，全程逐题提问用户确认。

### 阶段 1：数据发现

1. **源文件清单**：问用户有几个文件/几个 sheet，各自用途。
2. **表结构判定**：AI 自动探测 + 用户确认
   - 每行 = 一条记录？（常见，直接批量处理）
   - 一条记录跨多行？（如"藏品 + 多个角色"拆行 → 需按主键合并成一条）
   - 一行含多条记录？（如价格清单横排 → 需拆列成多条）
   - 表头在第几行？有无合并单元格？
3. **多表关系**：多个 sheet 是同一批记录（结构相同 → 合并一份导入），还是用途/结构不同？

### 阶段 2：列映射确认

- 每个结构不同的表，分别展示「源表头 → 目标字段」映射，请用户确认/修正。
- 歧义列（"系列"、"标签"等可能对应多字段）强制用户选择。

### 阶段 3：策略选择

- 不同结构的子表：**合并成一个导入包**（缺字段填默认值）还是**每个子表单独一个 ZIP**？
- 跨行记录：是否按某列（编号/名称）作为主键合并？

### 阶段 4：转换执行

按选定策略清洗并生成各表 `manifest.json`。多子表合并时先统一字段再合并；分开时各自生成。图片不处理（`imageFilenames` 留 `[]`）。

### 阶段 5：打包

PowerShell `Compress-Archive -Path .\manifest.json -DestinationPath <名字>.zip`，保证 `manifest.json` 在 ZIP 根目录。多 ZIP 场景逐个打包。

### 阶段 6：校验 + 报告

- 解压回读 `manifest.json`，验证 JSON 可解析、字段类型正确、`name` 非空、枚举合法。
- 检查 ZIP 限制：`manifest.json` ≤5MB、条目 ≤2000、单条目 ≤20MB、总 ≤200MB。
- 报告导入统计：总数、跳过的空行、转换的字段、可疑值、重复记录提醒、回退默认值的枚举。
- 提示用户用 App「导入备份」选择生成的文件。

## 表头别名映射

内置完整别名表（复用 `convert_excel.py:62-87` 并扩充常见变体）。模糊匹配规则：表头按包含关系匹配别名，多义时交给用户确认。核心字段别名示例：

- 名称 → `name`（名称/名字/商品名/品名）
- 种类 → `category`（种类/类别/分类/类型大类）
- 类型 → `type`（官方/散货等）
- IP/系列名 → `ipName`（IP/系列/IP名/作品）
- 制品系列 → `seriesName`（制品系列/系列名）
- 角色/标签 → `characterTag`（角色/标签/人物）
- 备注 → `remark`
- 购买渠道 → `purchaseChannel`（渠道/购买渠道）
- 购买店铺 → `purchaseShop`（店铺/购买店铺/店名）
- 购买日期 → `purchaseDate`（日期/入手日期）
- 购买价格 → `purchasePrice`（原价/收价/入手价/价格）
- 购买数量 → `purchaseQuantity`（数量）
- 购买运费 → `purchaseShipping`（运费）
- 预期售价 → `expectedPrice`（意愿/预期售价/期望价）
- 售出价格 → `sellPrice`（出价/售价/卖出价）
- 售出数量 → `sellQuantity`
- 售出运费 → `sellShipping`
- 包邮 → `isFreeShipping`（freeShipping/邮费包）
- 售出日期 → `sellDate`
- 买家信息 → `buyerInfo`（买家）
- 售出备注 → `sellRemark`
- 状态 → `status`（状态/拥有状态）
- 仓储状态 → `storageStatus`（仓储/存放状态）

## 解析清洗规则

- **日期**：`YYYY-MM-DD`、`YYYY/MM/DD`、`YYYY年M月D日`、`YYYY-MM`（日为 1）、Excel 序列号（>40000，基准 1899-12-30）、已是毫秒的数（≤40000）→ epoch 毫秒。
- **价格**：去 `¥￥,，` 取首个数字 token → Double；空 → 0.0。
- **数量**：首个整数 token → Int；空 → 1。
- **布尔**：`true/yes/y/是/1/t` → true，否则 false。
- **状态中文别名**：已出→SOLD、犹豫出→HESITATING_SELL、待出→LISTED、持有→OWNED、待补→PENDING_TAIL、待邮→PENDING_SHIPPING_FEE、待发货→PENDING_SEND、运输中→IN_TRANSIT_ORDER、赠品→GIFT、遗失/损坏→LOST；未知/空→OWNED（提示）。
- **仓储中文别名**：现货→IN_STOCK、在途→IN_TRANSIT、团长→GROUP_STORAGE、代购处囤货→AGENT_STORAGE；未知/空→IN_STOCK（提示）。
- **空行跳过**；`name` 为空的行跳过。
- 缺失 `purchaseDate` → 现在；`expectedPrice` → sellPrice 或 0.0。

## 边界情况

- **WPS `.et`、老 `.xls`**：无法直接读取 → 引导用户先在 WPS/Excel 中另存为 `.xlsx` 或 `.csv`。
- **CSV 编码**：自动检测 UTF-8 BOM / UTF-8 / GBK / GB18030，中文乱码时切换编码重读。
- **合并单元格、多行表头**：自动探测表头行（前若干行内含多个已识别列名的行）。
- **歧义列**：强制用户确认。
- **跨行记录**：按用户指定的主键列合并；合并时非主键单元格填充。

## 存放位置与格式

- 单文件：`.claude/skills/import-data-converter/SKILL.md`
- frontmatter：`name`、`description`（中文，说明触发条件与产出）、`license: MIT`
- 采用方案 A（单文件自包含），不拆分参考文件，不附带脚本。

## 验收标准

1. skill 文件可被 opencode 自动发现（与 build-apk 同级）。
2. skill 内容含完整 schema、别名映射、解析规则、交互式流程、校验清单。
3. 对用户提供的任意结构源文件，能按上述流程提问 → 转换 → 生成可直接导入的 ZIP。
