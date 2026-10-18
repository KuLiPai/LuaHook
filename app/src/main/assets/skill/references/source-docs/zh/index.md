---
layout: home

hero:
  name: LuaHook
  text: 通过 Lua 编写 Xposed 模块
  tagline: 任何人都能 Hook
  image:
    src: /logo.png
    alt: LuaHook
    template: |
      <div class="image-container">
        <div class="image-wrapper">
          <div class="image-glow"></div>
          <img src="%s" alt="%s" class="image-src">
        </div>
      </div>
  actions:
    - theme: brand
      text: 快速开始
      link: /quickstart
    - theme: alt
      text: 在 GitHub 查看
      link: https://github.com/kulipai/luahook

features:
  - icon: 🚀
    title: 极速运行
    details: 无需重新编译或重新安装应用，即可立即加载并运行 Lua 脚本。
  - icon: 🔧
    title: Lua 驱动的简洁性
    details: 使用简洁、灵活且对初学者友好的 Lua 脚本编写强大的 Xposed 模块。
  - icon: 💪
    title: 实时 + 持久化
    details: 结合了实时脚本和持久化 Hook，提供无缝的开发体验。
---
