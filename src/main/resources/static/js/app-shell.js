(function () {
    const STORAGE_KEYS = {
        username: "seckill_username",
        token: "seckill_login_token",
        loginAt: "seckill_login_at",
        sessionMaxAgeMs: "seckill_session_max_age_ms",
        themeMode: "seckill_theme_mode"
    };

    const LOGIN_TOKEN_PREFIX = "login_success_";
    const DEFAULT_SESSION_MAX_AGE_MS = 12 * 60 * 60 * 1000;

    function injectBaseStyle() {
        if (document.getElementById("seckill-top-nav-style")) {
            return;
        }
        const style = document.createElement("style");
        style.id = "seckill-top-nav-style";
        style.textContent = ""
            + ":root{--ui-radius-lg:16px;--ui-radius-md:12px;--ui-card-pad:16px;--ui-gap:12px;--ui-text:#1f2937;--ui-muted:#64748b;--ui-border:#e5e7eb;--ui-shadow:0 8px 24px rgba(15,23,42,.07);--ui-shadow-hover:0 14px 30px rgba(15,23,42,.12);--ui-primary:#2563eb}"
            + ".ui-compact .page{gap:var(--ui-gap)}"
            + ".ui-compact .card{border-radius:var(--ui-radius-lg)!important;padding:var(--ui-card-pad)!important;border:1px solid var(--ui-border)!important;box-shadow:var(--ui-shadow)!important}"
            + ".ui-compact .btn,.ui-compact .chip,.ui-compact .tab{font-size:13px}"
            + ".ui-compact .muted{color:var(--ui-muted)}"
            + ".ui-motion{background-size:140% 140%;animation:uiBgShift 18s ease-in-out infinite}"
            + ".ui-motion .card{transition:transform .22s ease, box-shadow .22s ease}"
            + ".ui-motion .card:hover{transform:translateY(-2px);box-shadow:var(--ui-shadow-hover)!important}"
            + ".ui-motion .fx-enter{animation:uiFadeUp .45s ease both;animation-delay:var(--enter-delay,0ms)}"
            + ".ui-motion .fx-pulse{animation:uiPulse 2.2s ease-in-out infinite}"
            + ".top-nav{display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;padding:10px 12px;border:1px solid #e5e7eb;border-radius:14px;background:#fff;box-shadow:0 4px 14px rgba(15,23,42,.04)}"
            + ".top-nav-brand{display:flex;align-items:center;gap:8px}"
            + ".top-nav-dot{width:9px;height:9px;border-radius:999px;background:#2563eb}"
            + ".top-nav-title{font-weight:700;color:#1f2937;font-size:14px}"
            + ".top-nav-sub{font-size:12px;color:#6b7280}"
            + ".top-nav-links{display:flex;align-items:center;gap:6px;flex-wrap:wrap}"
            + ".top-nav-actions{display:flex;align-items:center;gap:6px}"
            + ".top-link{border:1px solid #dbe2f0;border-radius:999px;padding:6px 12px;background:#f8fbff;color:#1d4ed8;text-decoration:none;font-size:13px;transition:all .2s ease}"
            + ".top-link.active{background:#2563eb;color:#fff;border-color:#2563eb}"
            + ".top-nav-toggle{display:none;border:1px solid #dbe2f0;border-radius:8px;padding:6px 10px;background:#fff;color:#334155;font-size:12px;cursor:pointer}"
            + ".theme-quick{border:1px solid #dbe2f0;border-radius:999px;width:32px;height:32px;background:#fff;color:#1d4ed8;font-size:14px;cursor:pointer;display:inline-flex;align-items:center;justify-content:center;line-height:1;transition:all .2s ease;position:relative}"
            + ".theme-quick:hover{background:#eff6ff}"
            + ".theme-quick::after{content:attr(data-tip);position:absolute;left:50%;top:40px;transform:translateX(-50%);white-space:nowrap;background:#0f172a;color:#fff;padding:4px 8px;border-radius:6px;font-size:11px;opacity:0;pointer-events:none;transition:opacity .2s ease;z-index:40}"
            + ".theme-quick:hover::after{opacity:1}"
            + ".top-nav-center{display:flex;align-items:center;gap:8px;flex-wrap:wrap}"
            + ".top-user-wrap{position:relative}"
            + ".top-user-trigger{border:1px solid #dbe2f0;border-radius:999px;padding:6px 10px;background:#fff;color:#334155;cursor:pointer;font-size:12px;display:inline-flex;align-items:center;gap:4px}"
            + ".top-user-trigger:hover{background:#f8fbff}"
            + ".top-user-menu{position:absolute;right:0;top:36px;min-width:170px;background:#fff;border:1px solid #e5e7eb;border-radius:10px;box-shadow:0 8px 20px rgba(15,23,42,.12);padding:6px;z-index:30}"
            + ".top-user-name{padding:7px 9px;font-size:12px;color:#475569;border-bottom:1px solid #f1f5f9;margin-bottom:4px}"
            + ".top-menu-item{width:100%;text-align:left;border:0;border-radius:8px;padding:7px 9px;background:#fff;color:#1d4ed8;cursor:pointer;font-size:12px;text-decoration:none;display:block}"
            + ".top-menu-item:hover{background:#eff6ff}"
            + ".top-menu-item.logout{color:#b91c1c}"
            + "@keyframes uiBgShift{0%{background-position:0% 20%}50%{background-position:100% 80%}100%{background-position:0% 20%}}"
            + "@keyframes uiFadeUp{0%{opacity:0;transform:translateY(8px)}100%{opacity:1;transform:translateY(0)}}"
            + "@keyframes uiPulse{0%,100%{transform:scale(1)}50%{transform:scale(1.03)}}"
            + "@media (prefers-reduced-motion: reduce){.ui-motion,.ui-motion .fx-enter,.ui-motion .fx-pulse{animation:none!important}.ui-motion .card,.top-link{transition:none!important}}"
            + ":root[data-theme='dark'] body,:root[data-theme='dark']{background:#0b1220!important;color:#e5e7eb!important}"
            + ":root[data-theme='dark'] .card,:root[data-theme='dark'] .panel,:root[data-theme='dark'] .mini,:root[data-theme='dark'] .item,:root[data-theme='dark'] .hero,:root[data-theme='dark'] .search-panel{background:#111a2d!important;border-color:#263248!important;color:#e5e7eb!important}"
            + ":root[data-theme='dark'] .top-nav{background:#111a2d!important;border-color:#263248!important;box-shadow:0 6px 20px rgba(0,0,0,.35)!important}"
            + ":root[data-theme='dark'] .top-nav-title{color:#e5e7eb!important}:root[data-theme='dark'] .top-nav-sub{color:#94a3b8!important}"
            + ":root[data-theme='dark'] .top-link{background:#0f1a30!important;border-color:#2c3a57!important;color:#93c5fd!important}"
            + ":root[data-theme='dark'] .top-link.active{background:#2563eb!important;border-color:#2563eb!important;color:#fff!important}"
            + ":root[data-theme='dark'] .theme-quick{background:#0f1a30!important;border-color:#2c3a57!important;color:#93c5fd!important}"
            + ":root[data-theme='dark'] .top-user-trigger{background:#0f1a30!important;border-color:#2c3a57!important;color:#dbeafe!important}"
            + ":root[data-theme='dark'] .top-user-menu{background:#0f1a30!important;border-color:#2c3a57!important}"
            + ":root[data-theme='dark'] .top-user-name{color:#cbd5e1!important;border-bottom-color:#24324a!important}"
            + ":root[data-theme='dark'] .top-menu-item{background:#0f1a30!important;color:#93c5fd!important}"
            + ":root[data-theme='dark'] .top-menu-item:hover{background:#1b2a44!important}"
            + ":root[data-theme='dark'] .top-menu-item.logout{color:#fca5a5!important}"
            + ":root[data-theme='dark'] .muted,:root[data-theme='dark'] .panel-sub,:root[data-theme='dark'] .meta-secondary,:root[data-theme='dark'] .field-help{color:#94a3b8!important}"
            + ":root[data-theme='dark'] .panel-main,:root[data-theme='dark'] h1,:root[data-theme='dark'] h2,:root[data-theme='dark'] strong,:root[data-theme='dark'] .price{color:#f8fafc!important}"
            + ":root[data-theme='dark'] .tag,:root[data-theme='dark'] .brand-tag,:root[data-theme='dark'] .status{background:#1e3a8a!important;color:#bfdbfe!important}"
            + ":root[data-theme='dark'] .badge.active{background:#14532d!important;color:#86efac!important}"
            + ":root[data-theme='dark'] .badge.upcoming{background:#713f12!important;color:#fde68a!important}"
            + ":root[data-theme='dark'] .badge.soldout,:root[data-theme='dark'] .badge.ended,:root[data-theme='dark'] .status.failed{background:#7f1d1d!important;color:#fecaca!important}"
            + ":root[data-theme='dark'] .input,:root[data-theme='dark'] .select,:root[data-theme='dark'] .search-input,:root[data-theme='dark'] .search-select{background:#0f1a30!important;border-color:#2c3a57!important;color:#e5e7eb!important}"
            + ":root[data-theme='dark'] .input::placeholder,:root[data-theme='dark'] .search-input::placeholder{color:#7b8ca8!important}"
            + ":root[data-theme='dark'] .btn.secondary,:root[data-theme='dark'] .btn.soft,:root[data-theme='dark'] .chip,:root[data-theme='dark'] .tab,:root[data-theme='dark'] .top-nav-toggle,:root[data-theme='dark'] .icon-btn,:root[data-theme='dark'] .fold-toggle{background:#0f1a30!important;border-color:#2c3a57!important;color:#93c5fd!important}"
            + ":root[data-theme='dark'] .btn.primary,:root[data-theme='dark'] .chip.active,:root[data-theme='dark'] .tab.active{background:#2563eb!important;border-color:#2563eb!important;color:#fff!important}"
            + ":root[data-theme='dark'] .notice.info,:root[data-theme='dark'] .message.info{background:#1e3a8a!important;color:#bfdbfe!important}"
            + ":root[data-theme='dark'] .notice.ok,:root[data-theme='dark'] .message.success{background:#14532d!important;color:#86efac!important}"
            + ":root[data-theme='dark'] .message.error{background:#7f1d1d!important;color:#fecaca!important}"
            + ":root[data-theme='dark'] .stock{background:#23324a!important}:root[data-theme='dark'] .stock>div{background:#22c55e!important}"
            + "@media (prefers-color-scheme: dark){"
            + ":root[data-theme='system'] body,:root[data-theme='system']{background:#0b1220!important;color:#e5e7eb!important}"
            + ":root[data-theme='system'] .card,:root[data-theme='system'] .panel,:root[data-theme='system'] .mini,:root[data-theme='system'] .item,:root[data-theme='system'] .hero,:root[data-theme='system'] .search-panel{background:#111a2d!important;border-color:#263248!important;color:#e5e7eb!important}"
            + ":root[data-theme='system'] .top-nav{background:#111a2d!important;border-color:#263248!important;box-shadow:0 6px 20px rgba(0,0,0,.35)!important}"
            + ":root[data-theme='system'] .top-nav-title{color:#e5e7eb!important}:root[data-theme='system'] .top-nav-sub{color:#94a3b8!important}"
            + ":root[data-theme='system'] .top-link{background:#0f1a30!important;border-color:#2c3a57!important;color:#93c5fd!important}"
            + ":root[data-theme='system'] .top-link.active{background:#2563eb!important;border-color:#2563eb!important;color:#fff!important}"
            + ":root[data-theme='system'] .theme-quick{background:#0f1a30!important;border-color:#2c3a57!important;color:#93c5fd!important}"
            + ":root[data-theme='system'] .top-user-trigger{background:#0f1a30!important;border-color:#2c3a57!important;color:#dbeafe!important}"
            + ":root[data-theme='system'] .top-user-menu{background:#0f1a30!important;border-color:#2c3a57!important}"
            + ":root[data-theme='system'] .top-user-name{color:#cbd5e1!important;border-bottom-color:#24324a!important}"
            + ":root[data-theme='system'] .top-menu-item{background:#0f1a30!important;color:#93c5fd!important}"
            + ":root[data-theme='system'] .top-menu-item:hover{background:#1b2a44!important}"
            + ":root[data-theme='system'] .top-menu-item.logout{color:#fca5a5!important}"
            + ":root[data-theme='system'] .muted,:root[data-theme='system'] .panel-sub,:root[data-theme='system'] .meta-secondary,:root[data-theme='system'] .field-help{color:#94a3b8!important}"
            + ":root[data-theme='system'] .panel-main,:root[data-theme='system'] h1,:root[data-theme='system'] h2,:root[data-theme='system'] strong,:root[data-theme='system'] .price{color:#f8fafc!important}"
            + ":root[data-theme='system'] .tag,:root[data-theme='system'] .brand-tag,:root[data-theme='system'] .status{background:#1e3a8a!important;color:#bfdbfe!important}"
            + ":root[data-theme='system'] .badge.active{background:#14532d!important;color:#86efac!important}"
            + ":root[data-theme='system'] .badge.upcoming{background:#713f12!important;color:#fde68a!important}"
            + ":root[data-theme='system'] .badge.soldout,:root[data-theme='system'] .badge.ended,:root[data-theme='system'] .status.failed{background:#7f1d1d!important;color:#fecaca!important}"
            + ":root[data-theme='system'] .input,:root[data-theme='system'] .select,:root[data-theme='system'] .search-input,:root[data-theme='system'] .search-select{background:#0f1a30!important;border-color:#2c3a57!important;color:#e5e7eb!important}"
            + ":root[data-theme='system'] .input::placeholder,:root[data-theme='system'] .search-input::placeholder{color:#7b8ca8!important}"
            + ":root[data-theme='system'] .btn.secondary,:root[data-theme='system'] .btn.soft,:root[data-theme='system'] .chip,:root[data-theme='system'] .tab,:root[data-theme='system'] .top-nav-toggle,:root[data-theme='system'] .icon-btn,:root[data-theme='system'] .fold-toggle{background:#0f1a30!important;border-color:#2c3a57!important;color:#93c5fd!important}"
            + ":root[data-theme='system'] .btn.primary,:root[data-theme='system'] .chip.active,:root[data-theme='system'] .tab.active{background:#2563eb!important;border-color:#2563eb!important;color:#fff!important}"
            + ":root[data-theme='system'] .notice.info,:root[data-theme='system'] .message.info{background:#1e3a8a!important;color:#bfdbfe!important}"
            + ":root[data-theme='system'] .notice.ok,:root[data-theme='system'] .message.success{background:#14532d!important;color:#86efac!important}"
            + ":root[data-theme='system'] .message.error{background:#7f1d1d!important;color:#fecaca!important}"
            + ":root[data-theme='system'] .stock{background:#23324a!important}:root[data-theme='system'] .stock>div{background:#22c55e!important}"
            + "}"
            + "@media (max-width: 760px){"
            + ".top-nav{align-items:flex-start}"
            + ".top-nav-toggle{display:inline-flex}"
            + ".top-nav-center{width:100%;display:none;justify-content:space-between;gap:10px}"
            + ".top-nav-center.open{display:flex;flex-direction:column;align-items:stretch}"
            + ".top-nav-links{width:100%}"
            + ".top-nav-actions{width:100%;justify-content:space-between}"
            + ".top-link{flex:1;text-align:center}"
            + ".theme-quick{width:34px;height:34px;align-self:center}"
            + ".top-user-wrap{width:100%}"
            + ".top-user-trigger{width:100%;justify-content:center}"
            + ".top-user-menu{left:0;right:auto;width:100%}"
            + "}";
        document.head.appendChild(style);
    }

    function normalizeAgeMs(raw) {
        const value = Number(raw);
        if (!Number.isFinite(value) || value <= 0) {
            return null;
        }
        return Math.floor(value);
    }

    function getSessionMaxAgeMs() {
        const runtimeConfig = window.__SECKILL_CONFIG__ || {};
        const fromRuntime = normalizeAgeMs(runtimeConfig.sessionMaxAgeMs);
        if (fromRuntime) {
            return fromRuntime;
        }
        const fromStorage = normalizeAgeMs(localStorage.getItem(STORAGE_KEYS.sessionMaxAgeMs));
        if (fromStorage) {
            return fromStorage;
        }
        return DEFAULT_SESSION_MAX_AGE_MS;
    }

    function setSessionMaxAgeMs(ms) {
        const value = normalizeAgeMs(ms);
        if (!value) {
            localStorage.removeItem(STORAGE_KEYS.sessionMaxAgeMs);
            return;
        }
        localStorage.setItem(STORAGE_KEYS.sessionMaxAgeMs, String(value));
    }

    function getThemeMode() {
        const mode = (localStorage.getItem(STORAGE_KEYS.themeMode) || "system").trim();
        if (mode === "light" || mode === "dark" || mode === "system") {
            return mode;
        }
        return "system";
    }

    function applyTheme(mode) {
        const finalMode = mode === "light" || mode === "dark" || mode === "system" ? mode : "system";
        document.documentElement.setAttribute("data-theme", finalMode);
    }

    function setThemeMode(mode) {
        const finalMode = mode === "light" || mode === "dark" || mode === "system" ? mode : "system";
        localStorage.setItem(STORAGE_KEYS.themeMode, finalMode);
        applyTheme(finalMode);
    }

    function systemPrefersDark() {
        return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
    }

    function isDarkModeActive(mode) {
        const sourceMode = mode || getThemeMode();
        if (sourceMode === "dark") {
            return true;
        }
        if (sourceMode === "light") {
            return false;
        }
        return systemPrefersDark();
    }

    function clearSession() {
        localStorage.removeItem(STORAGE_KEYS.username);
        localStorage.removeItem(STORAGE_KEYS.token);
        localStorage.removeItem(STORAGE_KEYS.loginAt);
    }

    function saveSession(username, token) {
        if (username) {
            localStorage.setItem(STORAGE_KEYS.username, username);
        }
        if (token && token.startsWith(LOGIN_TOKEN_PREFIX)) {
            localStorage.setItem(STORAGE_KEYS.token, token);
            localStorage.setItem(STORAGE_KEYS.loginAt, String(Date.now()));
        }
    }

    function getUsername() {
        return localStorage.getItem(STORAGE_KEYS.username) || "";
    }

    function getToken() {
        const token = localStorage.getItem(STORAGE_KEYS.token) || "";
        return token.startsWith(LOGIN_TOKEN_PREFIX) ? token : "";
    }

    function getUserId() {
        const token = getToken();
        return token ? token.replace(LOGIN_TOKEN_PREFIX, "") : "";
    }

    function isTokenExpired() {
        const token = getToken();
        if (!token) {
            return false;
        }
        const loginAtRaw = localStorage.getItem(STORAGE_KEYS.loginAt) || "";
        const loginAt = Number(loginAtRaw);
        if (!Number.isFinite(loginAt) || loginAt <= 0) {
            return false;
        }
        return Date.now() - loginAt > getSessionMaxAgeMs();
    }

    function handleExpiredIfNeeded() {
        if (!isTokenExpired()) {
            return false;
        }
        clearSession();
        return true;
    }

    function redirectToLogin(reason) {
        const url = "/index.html?reason=" + encodeURIComponent(reason || "unauthorized");
        window.location.href = url;
    }

    function readLoginHint() {
        const url = new URL(window.location.href);
        const reason = (url.searchParams.get("reason") || "").trim();
        if (reason === "expired") {
            return "登录态已过期，请重新登录。";
        }
        if (reason === "unauthorized") {
            return "请先登录后再继续。";
        }
        return "";
    }

    function ensureAuth(options) {
        const opts = options || {};
        const requireToken = Boolean(opts.requireToken);
        if (handleExpiredIfNeeded()) {
            redirectToLogin("expired");
            return false;
        }
        if (!requireToken) {
            return true;
        }
        if (!getToken()) {
            redirectToLogin("unauthorized");
            return false;
        }
        return true;
    }

    const TopNav = {
        props: {
            active: { type: String, default: "" },
            username: { type: String, default: "" },
            showLogout: { type: Boolean, default: true },
            mode: { type: String, default: "user" }
        },
        data() {
            return {
                menuOpen: false,
                navOpen: false,
                themeMode: getThemeMode()
            };
        },
        computed: {
            navItems() {
                if (this.mode === "guest") {
                    return [
                        { key: "index", text: "登录", href: "/index.html" },
                        { key: "register", text: "注册", href: "/index.html#register" }
                    ];
                }
                return [
                    { key: "main", text: "控制台", href: "/main.html" },
                    { key: "products", text: "商品", href: "/products.html" },
                    { key: "orders", text: "订单", href: "/orders.html" }
                ];
            },
            quickThemeIcon() {
                return isDarkModeActive(this.themeMode) ? "☀" : "☾";
            },
            quickThemeText() {
                return isDarkModeActive(this.themeMode) ? "切到浅色" : "切到深色";
            }
        },
        methods: {
            toggleMenu() {
                this.menuOpen = !this.menuOpen;
            },
            toggleNav() {
                this.navOpen = !this.navOpen;
            },
            closeMenu() {
                this.menuOpen = false;
            },
            goLogin() {
                redirectToLogin("unauthorized");
            },
            logout() {
                clearSession();
                redirectToLogin("unauthorized");
            },
            setTheme(mode) {
                this.themeMode = mode;
                setThemeMode(mode);
            },
            toggleThemeQuick() {
                const nextMode = isDarkModeActive(this.themeMode) ? "light" : "dark";
                this.setTheme(nextMode);
            },
            onDocClick(event) {
                if (!this.menuOpen) {
                    return;
                }
                if (!this.$el.contains(event.target)) {
                    this.closeMenu();
                    this.navOpen = false;
                }
            }
        },
        mounted() {
            document.addEventListener("click", this.onDocClick);
        },
        unmounted() {
            document.removeEventListener("click", this.onDocClick);
        },
        template: ""
            + "<header class='top-nav'>"
            + "  <div class='top-nav-brand'>"
            + "    <span class='top-nav-dot'></span>"
            + "    <div>"
            + "      <div class='top-nav-title'>秒杀商城</div>"
            + "      <div class='top-nav-sub'>用户端</div>"
            + "    </div>"
            + "  </div>"
            + "  <button class='top-nav-toggle' type='button' @click='toggleNav'>"
            + "    {{ navOpen ? '收起菜单' : '展开菜单' }}"
            + "  </button>"
            + "  <div class='top-nav-center' :class='{open: navOpen}'>"
            + "    <nav class='top-nav-links'>"
            + "      <a v-for='item in navItems' :key='item.key' class='top-link' :class='{active: active===item.key}' :href='item.href'>{{ item.text }}</a>"
            + "    </nav>"
            + "    <div class='top-nav-actions'>"
            + "      <button class='theme-quick' type='button' :title='quickThemeText' :data-tip='quickThemeText' :aria-label='quickThemeText' @click='toggleThemeQuick'>"
            + "        <span aria-hidden='true'>{{ quickThemeIcon }}</span>"
            + "      </button>"
            + "      <div class='top-nav-links top-user-wrap'>"
            + "    <button class='top-user-trigger' type='button' @click='toggleMenu'>"
            + "      {{ username ? ('当前用户: ' + username) : '账户菜单' }}"
            + "      <span>{{ menuOpen ? '▲' : '▼' }}</span>"
            + "    </button>"
            + "    <div v-if='menuOpen' class='top-user-menu'>"
            + "      <div class='top-user-name'>{{ username || '未登录用户' }}</div>"
            + "      <button class='top-menu-item' type='button' @click='goLogin'>回登录页</button>"
            + "      <button v-if='showLogout' class='top-menu-item logout' type='button' @click='logout'>退出登录</button>"
            + "    </div>"
            + "  </div>"
            + "    </div>"
            + "  </div>"
            + "</header>"
    };

    injectBaseStyle();
    applyTheme(getThemeMode());

    window.SeckillUi = {
        STORAGE_KEYS,
        LOGIN_TOKEN_PREFIX,
        SESSION_MAX_AGE_MS: DEFAULT_SESSION_MAX_AGE_MS,
        TopNav,
        getSessionMaxAgeMs,
        setSessionMaxAgeMs,
        getThemeMode,
        setThemeMode,
        applyTheme,
        isDarkModeActive,
        saveSession,
        clearSession,
        getUsername,
        getToken,
        getUserId,
        isTokenExpired,
        handleExpiredIfNeeded,
        ensureAuth,
        redirectToLogin,
        readLoginHint
    };
})();

