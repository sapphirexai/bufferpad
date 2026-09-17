# 维护与发布流程

[贡献指南](../CONTRIBUTING.md) · [CI 范围](ci.md) · [路线图](../ROADMAP.md)

## 日常修改

1. 从最新 main 创建工作分支，按实际问题提交独立修改。
2. 向 main 创建 PR，说明行为、测试和未验证范围。
3. 等待 `Frontend`、`Repository checks`、`PowerShell` 三个 GitHub Actions 检查通过；main 有更新时先同步。
4. 处理未解决讨论并审阅差异后合并。单维护者不强制第二人批准，但管理员也必须遵守检查；禁止直接强推或删除 main。

保留普通 merge 历史，不要求把迁移合并提交改成线性历史。`archive/` 分支是清理后的来源记录，不作为开发目标，不把未清理的原始 GitLab 历史重新合回公开仓库。

## 发布源码版本

1. 更新 `CHANGELOG.md` 和对应 `docs/releases/` 说明，区分已验证和未验证范围。
2. 在 PR/CI 通过后选择 main 的准确提交，建立版本标签和 GitHub Release。
3. 预览版本标为 prerelease；标签与说明都对应实际代码，不把旧开发日期当成首次公开日期。
4. 首版只提供 GitHub 自动生成源码归档，不附带 HSL、第三方安装器或包含它们的成品。后续制品发布须先核对实际内容与再分发条件。
5. 从远端重新克隆，核对标签、文件、依赖说明和 CI。GitHub 源码 ZIP 不包含 Git 历史，需要完整记录时使用 clone。

GitHub 设置不能仅靠 clone 备份。调整分支保护、Topics、私密报告入口时记录变更，并回读确认。
