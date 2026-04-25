# Design System Master File (Updated)

> **LOGIC:** When building a specific page, first check `design-system/pages/[page-name].md`.
> If that file exists, its rules **override** this Master file.
> If not, strictly follow the rules below.

---

**Project:** Taara BMS
**Category:** Modern Analytics Dashboard
**Vibe:** Neat, bright, interesting, premium.

---

## Global Rules

### Color Palette

This palette uses a deep, rich indigo background with highly vibrant, luminous accents that make data pop elegantly in dark mode.

| Role | Hex | CSS Variable |
|------|-----|--------------|
| Primary/Accent | `#818CF8` | `--color-primary` (Indigo/Violet) |
| Secondary | `#34D399` | `--color-secondary` (Teal/Emerald) |
| Vibrant Highlight | `#F472B6` | `--color-highlight` (Pink/Coral) |
| Background | `#0B0F19` | `--color-background` (Deep Midnight) |
| Surface/Cards | `#131B2F` | `--color-surface` |
| Text Main | `#F8FAFC` | `--color-text` |
| Text Muted | `#94A3B8` | `--color-text-muted` |

### Typography

- **Heading Font:** Plus Jakarta Sans
- **Body Font:** Inter
- **Mood:** elegant, modern, extremely clean, highly legible.
- **Google Fonts:** [Plus Jakarta Sans + Inter](https://fonts.google.com/share?selection.family=Inter:wght@400;500;600|Plus+Jakarta+Sans:wght@500;600;700)

**CSS Import:**
```css
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=Plus+Jakarta+Sans:wght@500;600;700&display=swap');

body { font-family: 'Inter', sans-serif; }
h1, h2, h3, h4, h5, h6 { font-family: 'Plus Jakarta Sans', sans-serif; }
```

### Key Effects

- **Border Radius:** `12px` to `16px` for cards, `8px` for buttons.
- **Shadows:** Use glowing, colored drop shadows on active/hovered elements (e.g., `0 8px 16px rgba(129, 140, 248, 0.2)` on Primary).
- **Layout:** Generous padding (at least `24px` inside module cards). Provide enough whitespace so the colorful data metrics stand out clearly without feeling cramped.
