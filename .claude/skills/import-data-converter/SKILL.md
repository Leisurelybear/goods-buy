---
name: import-data-converter
description: Use when a user has existing collectible records in CSV/Excel/WPS spreadsheet files (various headers, messy values, possibly multiple sheets) and wants them converted into a ZIP backup package the GoodsBuy app can import via 导入备份. Triggers: importing/migrating existing records, 数据导入/迁移, converting Excel/CSV/WPS to import format, integrating a user's existing collection data.
license: MIT
---

# Import Data Converter（导入数据转换）

把用户已有的各种格式记录（CSV / Excel / WPS 等）转换为 App 可直接导入的 **ZIP 备份包**。

**核心原则：先问清数据结构，再选转换策略。绝不假设"一个文件 = 一张表 = 每行一条记录"。**

## 目标格式

ZIP 备份包 = 根目录一个 `manifest.json`（可选 `images/` 目录，本 skill 不处理图片）。

```json
{ "version": 1, "timestamp": 1786545728290, "collectibles": [ { ... } ] }
```

每条记录字段（与 `BackupManager.kt` 完全一致）：

| 字段 | 类型 | 必填 | 说明 | 默认值 |
|---|---|---|---|---|
| `name` | String | 是 | 名称 | — |
| `category` | String | 否 | 种类（手办/模型） | `""` |
| `type` | String | 否 | 类型（官方等） | `"官方"` |
| `ipName` | String | 否 | IP/系列名 | `""` |
| `seriesName` | String | 否 | 制品系列 | `""` |
| `characterTag` | String | 否 | 角色/标签 | `""` |
| `remark` | String | 否 | 备注 | `""` |
| `purchaseChannel` | String | 否 | 购买渠道 | `""` |
| `purchaseShop` | String | 否 | 购买店铺 | `""` |
| `purchaseDate` | Long | 否 | 购买日期，**epoch 毫秒** | 现在 |
| `purchasePrice` | Double | 否 | 购买价格（≥0） | `0.0` |
| `purchaseQuantity` | Int | 否 | 购买数量（>0） | `1` |
| `purchaseShipping` | Double | 否 | 购买运费（≥0） | `0.0` |
| `expectedPrice` | Double | 否 | 预期售价 | =sellPrice 或 `0.0` |
| `sellPrice` | Double? | 否 | 售出价格 | null |
| `sellQuantity` | Int? | 否 | 售出数量（≤购买数量） | null |
| `sellShipping` | Double? | 否 | 售出运费 | null |
| `isFreeShipping` | Boolean | 否 | 包邮 | `false` |
| `sellDate` | Long? | 否 | 售出日期，**epoch 毫秒** | null |
| `buyerInfo` | String? | 否 | 买家信息 | null |
| `sellRemark` | String? | 否 | 售出备注 | null |
| `status` | String | 否 | 状态英文枚举 | `OWNED` |
| `storageStatus` | String | 否 | 仓储英文枚举 | `IN_STOCK` |
| `imageFilenames` | List\<String\> | 否 | 图片文件名，本 skill 恒为 `[]` | `[]` |
| `createdAt` / `updatedAt` | Long | 否 | 创建/更新时间 | 现在 |

### 枚举值（必须用英文，非法值 App 静默回退默认）

- `status`：`PENDING_TAIL`待补尾款 · `PENDING_SHIPPING_FEE`待补邮 · `PENDING_SEND`待发货 · `IN_TRANSIT_ORDER`运输中 · `OWNED`已拥有 · `HESITATING_SELL`犹豫出售 · `LISTED`已挂出 · `SOLD`已售出 · `GIFT`赠品/付邮送 · `LOST`遗失/损坏
- `storageStatus`：`IN_STOCK`现货 · `IN_TRANSIT`在途 · `GROUP_STORAGE`团长囤货 · `AGENT_STORAGE`代购处囤货

### 去重指纹

App 导入时按 SHA-1(name, ipName, seriesName, characterTag, purchaseChannel, purchaseShop, remark, purchasePrice, purchaseQuantity, purchaseShipping, purchaseDate, status, storageStatus, sellPrice, sellDate, category, type) 判重。转换时：
- 若多行指纹**完全相同**，提醒用户可能重复。
- **跨文件/跨 sheet 同名提醒**：同一名称出现在多份输入时（字段可能不同、指纹不同），额外向用户提示，由用户决定是否合并成一条（如"购入记录"+"售出记录"拼成一条完整记录）。

## 表头别名映射

按**包含关系**模糊匹配源表头到目标字段。别名命中多个字段时（如"系列"既可能是 `ipName` 也可能是 `seriesName`）**必须问用户确认**，不得擅自选一个。

| 目标字段 | 别名关键词 |
|---|---|
| `name` | 名称、名字、商品名、品名、物品、item |
| `category` | 种类、类别、分类 |
| `type` | 类型 |
| `ipName` | IP、系列名、作品、系列（歧义） |
| `seriesName` | 制品系列、系列（歧义） |
| `characterTag` | 角色、标签、人物 |
| `remark` | 备注、说明 |
| `purchaseChannel` | 渠道、购买渠道 |
| `purchaseShop` | 店铺、购买店铺、店名 |
| `purchaseDate` | 购买日期、日期、入手日期 |
| `purchasePrice` | 购买价格、入手价、购入价、原价、价格 |
| `purchaseQuantity` | 数量、购买数量 |
| `purchaseShipping` | 运费、购买运费 |
| `expectedPrice` | 预期售价、意愿、期望价 |
| `sellPrice` | 售出价格、卖出价、出价、售价 |
| `sellQuantity` | 售出数量 |
| `sellShipping` | 售出运费 |
| `isFreeShipping` | 包邮、freeShipping |
| `sellDate` | 售出日期 |
| `buyerInfo` | 买家、买家信息 |
| `sellRemark` | 售出备注 |
| `status` | 状态、拥有状态 |
| `storageStatus` | 仓储状态、仓储、存放状态 |

## 解析清洗规则

- **日期** → Long(epoch 毫秒)：`YYYY-MM-DD`、`YYYY/MM/DD`、`YYYY年M月D日`、`YYYY-MM`（日=1）、Excel 序列号（>40000，基准 1899-12-30）。**纯数字**：只有明显是毫秒时间戳（约 1e12 量级）才直接当 epoch 毫秒；4 位纯数字（1900~2100 区间）视为**年份** → 当年 1 月 1 日；其他小数字按 Excel 序列号处理。空 → 现在。
- **价格/金额** → Double：去掉 `¥￥,，元` 等，取首个数字 token；未加引号的千分位逗号会把 CSV 拆列，先用正则 `(?<=\d),(?=\d{3}(?!\d))` 合并千分位（勿用裸 `\d,\d{3}`，会误伤 `数量,2025-01-01` 这类列边界）再解析。空 → `0.0`。
- **数量** → Int：首个整数 token；空 → `1`。
- **布尔** → `true`/`false`：`true/yes/y/是/1/t` 为 true，其余 false。
- **状态中文** → 英文枚举：已出→`SOLD`、犹豫出→`HESITATING_SELL`、待出→`LISTED`、持有/已拥有→`OWNED`、待补→`PENDING_TAIL`、待邮→`PENDING_SHIPPING_FEE`、待发货→`PENDING_SEND`、运输中→`IN_TRANSIT_ORDER`、赠品→`GIFT`、遗失/损坏→`LOST`。未知或空 → `OWNED`，**并提醒用户**。
- **仓储中文** → 英文枚举：现货→`IN_STOCK`、在途→`IN_TRANSIT`、团长→`GROUP_STORAGE`、代购处囤货→`AGENT_STORAGE`。未知或空 → `IN_STOCK`，**并提醒用户**。
- **空行跳过**；`name` 为空的记录行跳过。
- `expectedPrice` 缺省 = sellPrice 或 `0.0`；`type` 缺省 `"官方"`。
- **纯售出记录**（只有卖出信息、无购买信息）：`purchaseDate` 不要默认 now，用 `0` 或询问用户；并在报告中标注为可疑。
- **字段书写**：与 `BackupManager.buildJson` 一致，可空字段（sell* 等）为空时省略不写；显式写 null 也能被 App 接受，两种均可。
- **数值字段必须显式写入**：`purchaseQuantity`、`purchasePrice`、`purchaseShipping` 等即使源数据为空也要写出默认值（如 `purchaseQuantity: 1`），因为导入端 `optInt` 缺省返回 0，省略会让记录语义错误。

## 工作流（必须逐阶段执行，不得跳过提问）

### 阶段 1：数据发现（提问）

1. **源文件清单**：问用户提供几个文件/几个 sheet，各自用途。
2. **表结构判定**（AI 探测后问用户确认）：
   - 每行 = 一条记录？（最常见，直接批量）
   - 一条记录跨多行？（如"藏品 + 多个角色"拆行 → 需按主键列合并成一条）
   - 一行含多条记录？（如横排价格清单 → 需拆列成多条）
   - 表头在第几行？有无合并单元格？（自动探测：在前几行里找包含多个已知列名的行）
3. **多表关系**：多个 sheet 是同一批记录（结构相同 → 合并一份导入），还是用途/结构不同？

### 阶段 2：列映射确认

- 每个结构不同的表，分别展示「源表头 → 目标字段」映射，请用户确认/修正。
- 歧义列（"系列"、"标签"等）**强制用户选择**，禁止自行假设。

### 阶段 3：策略选择（提问）

- 不同结构的子表：**合并成一个导入包**（缺字段填默认值）还是**每个子表单独生成一个 ZIP**？
- 跨行记录：按哪个列（如编号/名称）作主键合并？
- 合并时重复的 `name` 如何处理（默认保留，App 导入时可去重）？

### 阶段 4：转换执行

按选定策略清洗生成 `manifest.json`。合并模式先统一字段再合并；分开模式各自生成。图片一律不处理，`imageFilenames` 恒为 `[]`。

### 阶段 5：打包

`manifest.json` 必须位于 ZIP **根目录**。用 PowerShell：

```powershell
Compress-Archive -Path .\manifest.json -DestinationPath "导出文件名.zip" -Force
```

多 ZIP 场景逐份打包，文件名用清晰中文，如 `我的藏品导入_手办.zip`。

### 阶段 6：校验与报告

1. 解压回读 `manifest.json`：JSON 可解析、`version==1`、字段类型正确、`name` 非空、`status`/`storageStatus` 是合法枚举。
2. 检查 ZIP 限制：manifest ≤5MB、条目 ≤2000、单条目 ≤20MB、总 ≤200MB。
3. 报告：总记录数、跳过的空行、回退默认值的枚举、可疑值、可能重复的记录。
4. 提示用户用 App「导入备份」选择生成的文件。

## 常见错误

- **假设单表**：不探测多 sheet / 跨行 / 横排结构就直接转换 → 先提问。
- **歧义列自行猜测**："系列" 直接映射 seriesName → 必须问用户。
- **日期写成字符串**：manifest 里日期必须是毫秒数字，不是 `"2025-03-15"`。
- **状态用中文或自造词**：必须是上方枚举的英文 key，否则 App 静默回退 `OWNED`。
- **CSV 千分位逗号拆列**：解析前先按 `(?<=\d),(?=\d{3}(?!\d))` 合并千分位，勿用裸 `\d,\d{3}`（会误伤列边界）。
- **`.et` / 老 `.xls` 读不了**：openpyxl 只支持 `.xlsx`。遇到 `.et`、`.xls` 先让用户在 WPS/Excel 里另存为 `.xlsx` 或 `.csv`。
- **CSV 乱码**：自动尝试 UTF-8 BOM / UTF-8 / GBK / GB18030 直到中文可读。
- **ZIP 里嵌套目录**：`manifest.json` 必须直接在根目录，App 才认。
