---
target: FrontEnd/QLBanMayTinh
total_score: 24
p0_count: 2
p1_count: 2
timestamp: 2026-07-09T08-47-22Z
slug: frontend-qlbanmaytinh
---
Method: dual-agent (A: general-purpose design review · B: general-purpose detector scan)

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 3/4 | Toasts, skeletons, cart badges present; but `placeOrder()` in CheckoutModal does 3+ sequential API calls with only a button-label swap — no progress signal for the highest-stakes flow in the app. |
| 2 | Match Between System / Real World | 3/4 | Vietnamese domain terms and familiar tab grouping; admin stat-card icons (💻🧾👥💰) are generic stock substitutes, not tied to real inventory semantics. |
| 3 | User Control and Freedom | 2/4 | No Escape-to-close on any modal; admin delete actions route through native `confirm()`/`alert()` (5+ call sites) instead of the app's own UI language. |
| 4 | Consistency and Standards | 2/4 | Two feedback systems for the same "confirm before destroy" moment: themed toast on storefront vs. raw browser dialogs on admin. |
| 5 | Error Prevention | 3/4 | Checkout validates client-side before hitting the backend; admin product/promo/staff forms submit straight to API with no required-field validation. |
| 6 | Recognition Rather Than Recall | 3/4 | `orderStatus.js` label/color/icon system reused identically across App, AccountPage, AdminPage — a real strength. |
| 7 | Flexibility and Efficiency of Use | 2/4 | Zero keyboard shortcuts; NavBar mega-menu is hover-only (`@mouseenter`/`@mouseleave`), no click/keyboard trigger — locks out power users and keyboard users from the biggest nav surface. |
| 8 | Aesthetic and Minimalist Design | 3/4 | Storefront dark/yellow theme is bold and consistent with "Overclock Arena"; admin dashboard is visually noisier than PRODUCT.md's "admin should be quieter" principle. |
| 9 | Error Recovery | 2/4 | Checkout's `parseApiError` converts raw JSON to readable text well; most admin CRUD failures dump raw backend text unfiltered into a red alert box. |
| 10 | Help and Documentation | 1/4 | No onboarding, no tooltips beyond native `title=`, no empty-state guidance beyond "no products found." |
| **Total** | | **24/40** | **Acceptable — significant improvements needed, solid bones underneath** |

## Anti-Patterns Verdict

**Start here: does this look AI-generated?** Mixed — the storefront mostly avoids the classic tells (no gradient text, no glassmorphism-as-default, no numbered 01/02/03 markers), but the **admin dashboard is a textbook hero-metric template**: four identical stat cards (icon chip in a tinted rgba circle + small label + big number) — exactly what DESIGN.md's Do's/Don'ts forbids. Every admin section header is also prefixed with a decorative emoji (🍩📅🔥🐌🩺📈🗃️🖥️🏷️🗂️💵🔖), a widely-recognized "LLM-generated dashboard" tell.

**LLM assessment (Assessment A)** independently flagged `AccountPage.vue:352` — a `border-left:4px solid` used as a color-coded status stripe — as a direct violation of DESIGN.md's "no border-left as decorative accent" rule.

**Deterministic scan (Assessment B)**: `detect.mjs` against `FrontEnd/QLBanMayTinh/src` found **134 findings** across 5 rules — and independently caught the *exact same* `AccountPage.vue:352` line as a `side-tab` violation, confirming the LLM's read with hard evidence:

| Rule | Severity | Count |
|---|---|---|
| `design-system-color` (color outside DESIGN.md) | advisory | 91 |
| `design-system-radius` (radius outside DESIGN.md) | advisory | 38 |
| `overused-font` | warning | 3 |
| `layout-transition` (animating a layout property) | warning | 1 |
| `side-tab` (decorative border-left accent) | warning | 1 |

Color/radius drift is concentrated in `AdminPage.vue` (51 color + 7 radius) and `CheckoutModal.vue` (22 color + 12 radius) — both files reach for one-off hex values (`#f4c200`, `#7f1d1d`, `#22c55e`, `#2a2a3a`) instead of the token system that `theme.css` and `DESIGN.md` already define. Two findings are lower-severity drift rather than clean violations: `App.vue:754`'s `rgba(0,0,0,0.4)` exactly matches the existing `--shadow-color` token value — it should reference the variable, not repeat its literal. `BarChart.vue:14`'s `transition: width` animates a layout-triggering CSS property (should animate `transform: scaleX()` instead for performance).

**Visual overlays**: unavailable this run — no browser automation tool was exposed in this session, so there's no live-page overlay to point you at. All findings above come from source inspection (this project styles almost everything inline, so this is a reasonably faithful substitute) and the deterministic CLI scan.

## Overall Impression

The storefront has real bones — a genuinely bold, on-brand dark/yellow theme, a well-thought-out cart/session model, and a single source of truth for order status reused consistently everywhere. But two things undercut it: **accessibility was designed for in DESIGN.md's prose but not implemented in code** (sitewide `outline: none` with zero compensating focus styles, one `aria-label` in the entire `src/` tree, a mouse-only mega-menu), and **the admin dashboard drifted from both the token system and PRODUCT.md's own "admin should be quieter" principle** — it's the loudest, busiest surface in the app despite being the one meant for fast, repeated staff use. The single biggest opportunity: fixing keyboard/focus support and admin visual noise would move the score from "acceptable" into "good" without touching the storefront's already-working identity.

## What's Working

- **`orderStatus.js` as single source of truth** — label/color/icon reused identically across App, AccountPage, and AdminPage's donut charts. Exactly the token discipline DESIGN.md asks for, and it's actually implemented, not aspirational.
- **Checkout's step-gated validation** (`goToPayment()`) catches missing fields client-side with specific per-field messages before ever hitting the network.
- **Per-account cart persistence** — `saophone_cart_${userId}` localStorage keys with guest fallback, restored on login. A subtle, well-thought-out piece of state design most storefront clones skip.

## Priority Issues

**[P0] No keyboard/focus story anywhere.**
Why it matters: `main.css:13` sets `input { outline: none; }` globally with zero compensating `:focus-visible` styles anywhere in the codebase, and there's exactly **one** `aria-label` in the entire `src/` tree. PRODUCT.md explicitly commits to WCAG AA keyboard navigation — this directly contradicts it, and blocks Sam (accessibility-dependent persona) from completing checkout at all.
Fix: remove the blanket `outline: none`, add `:focus-visible` rings using DESIGN.md's new "Accent Glow" token, add `aria-label`s to icon-only controls (theme toggle, cart's ✕, CartItem's +/− buttons, Modal's close button).
Suggested command: `/impeccable harden`

**[P0] Mega-menu is unreachable without a mouse.**
Why it matters: NavBar.vue's category dropdown opens only on `@mouseenter`/`@mouseleave` — no click handler, no `aria-expanded`/`aria-haspopup`, no Escape/Tab handling. Keyboard and touch-only users cannot access brand/category navigation at all, the single biggest nav surface in the storefront.
Fix: trigger on click as well as hover, add ARIA state, close on Escape.
Suggested command: `/impeccable harden`

**[P1] Checkout has no in-flight progress feedback for a multi-request order.**
Why it matters: `placeOrder()` performs sequential awaited calls (create customer → create order → N item inserts) with rollback-on-failure, but the UI shows only a static disabled button label. On a slow connection this is the highest-anxiety moment in the app with the weakest feedback — "did my order go through?"
Fix: add a step/progress indicator ("Đang tạo đơn... 2/4 sản phẩm").
Suggested command: `/impeccable harden`

**[P1] Admin destructive actions use native `confirm()`/`alert()` instead of the app's own UI, and bypass the token system.**
Why it matters: ~5 call sites in AdminPage.vue (delete product/customer/staff/promo/order) break out to browser-native dialogs — jarring, unbranded, and gives staff no specific "this cannot be undone" context. The detector's evidence corroborates a broader pattern: AdminPage.vue alone accounts for 51 of the 91 color-token bypasses and 7 of the 38 radius bypasses, meaning the inconsistency isn't just the confirm dialogs — the whole surface has drifted from the design system.
Fix: replace native dialogs with a themed confirm modal reusing `Modal.vue`; sweep one-off hex/radius values back onto DESIGN.md tokens while in there.
Suggested command: `/impeccable polish`

**[P2] Admin dashboard violates PRODUCT.md's own "admin should be quieter" principle.**
Why it matters: 4 stat cards + 2 donut charts + 2 bar charts + 3 gauges + 1 trend chart + a table all render at once with equal visual weight — the exact opposite of "tốc độ thao tác là ưu tiên" (speed of task completion is the priority) that PRODUCT.md commits to for staff users. It's also the surface most likely to read as "AI-generated" (hero-metric card grid + decorative emoji headers).
Fix: group into collapsible sections or a top KPI strip with drill-down instead of showing everything at once.
Suggested command: `/impeccable distill`

## Persona Red Flags

**Sam (accessibility-dependent)**: Fails hardest of all personas tested. Global `outline: none` on inputs, near-total absence of `aria-label`/`role`, mouse-only mega-menu, and modals with no `role="dialog"` / `aria-modal` / focus trap / Escape-to-close. A screen-reader or keyboard-only user cannot reliably complete checkout or navigate categories at all — directly contradicts PRODUCT.md's WCAG AA commitment.

**Alex (impatient power user, admin)**: No keyboard shortcuts anywhere in AdminPage; every row-level action requires precise mouse clicks followed by a native `confirm()` popup that steals focus out of the app's own flow — slow for someone processing dozens of orders a shift. The "everything visible at once" dashboard layout also forces scrolling past 4 chart blocks just to reach the recent-products table.

**Casey (distracted mobile user)**: NavBar's mega-menu is a hardcoded `width: 820px` with no visible mobile fallback in the component. The cart panel is a fixed `width: 390px` slide-in, which will exceed viewport width and clip/scroll horizontally on phones under ~410px wide.

## Minor Observations

- CheckoutModal's payment-info boxes use hardcoded dark-tinted hex colors (`#1e2a1e`, `#0a1a2a`, `#2a2000`, etc.) instead of theme tokens — will look muddy/wrong in light theme, contradicting PRODUCT.md's dual-theme requirement.
- LoginForm's "Quên mật khẩu?" link is `href="#"` with no handler — a dead affordance at a moment users are already frustrated.
- `App.vue:754`'s cart-panel shadow (`rgba(0,0,0,0.4)`) and `App.vue:568`'s overlay (`rgba(0,0,0,0.8)`) duplicate existing `--shadow-color`/`--bg-overlay` token values as literals instead of referencing them.
- `ProductCard.vue`'s badge chips use raw hex instead of CSS vars — acceptable per DESIGN.md's "accent is literal" stance, but inconsistent with the rest of the file, which uses `var(--...)` everywhere else.

## Questions to Consider

- If a keyboard-only or screen-reader user tried to complete a purchase today, where would they actually get stuck — has anyone traced that path end-to-end, or has accessibility only been designed for in DESIGN.md's prose?
- The admin dashboard renders every chart at once regardless of whether staff came in to process an order or check inventory — what if it adapted to the task instead of showing everything, all the time?
- Native `confirm()`/`alert()` in admin suggests the themed pattern wasn't extended there — was that a deliberate scope cut, or did it just not get reused?
