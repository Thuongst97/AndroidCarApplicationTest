(function () {
  "use strict";
  const data = JSON.parse(document.getElementById("atr-data").textContent);
  const extras = JSON.parse(document.getElementById("atr-extras").textContent);
  const info = data.info, stats = data.stats, tests = data.tests;
  const COLORS = { PASS: "var(--pass)", FAIL: "var(--fail)", ERROR: "var(--error)", SKIP: "var(--skip)" };
  const el = (tag, attrs = {}, ...kids) => {
    const e = document.createElement(tag);
    for (const [k, v] of Object.entries(attrs)) {
      if (k === "class") e.className = v; else if (k === "html") e.innerHTML = v;
      else if (k.startsWith("on")) e.addEventListener(k.slice(2), v); else if (v !== null && v !== undefined) e.setAttribute(k, v);
    }
    for (const k of kids.flat()) if (k !== null && k !== undefined) e.append(k.nodeType ? k : document.createTextNode(String(k)));
    return e;
  };
  const fmtDur = ms => { const s = ms / 1000; return s >= 60 ? `${Math.floor(s / 60)}m ${String(Math.round(s % 60)).padStart(2, "0")}s` : `${s.toFixed(1)}s`; };
  const fmtDate = ms => new Date(ms).toLocaleString();
  const simple = cls => cls.split(".").pop();
  const fullName = t => `${t.id.className}#${t.id.method}`;
  const runMs = info.finishedAt - info.startedAt;
  const ok = stats.failed + stats.errors === 0;

  // ---------- Header ----------
  document.getElementById("header").append(el("div", { class: "header-inner" },
    el("div", {},
      el("h1", {}, info.name),
      el("div", { class: "header-meta" },
        `${fmtDate(info.startedAt)} · ${fmtDur(runMs)} · ${info.device.manufacturer} ${info.device.model} · Android ${info.device.androidRelease} · ${info.device.serial}`)),
    el("div", { class: `verdict ${ok ? "pass" : "fail"}` }, ok ? "ALL TESTS PASSED" : `${stats.failed + stats.errors} FAILED`)));

  // ---------- KPIs ----------
  const kpi = (cls, v, l) => el("div", { class: `kpi ${cls}` }, el("div", { class: "v" }, v), el("div", { class: "l" }, l));
  document.getElementById("kpis").append(
    kpi("", stats.total, "Total"), kpi("pass", stats.passed, "Passed"), kpi("fail", stats.failed, "Failed"),
    kpi("error", stats.errors, "Errors"), kpi("skip", stats.skipped, "Skipped"), kpi("flaky", stats.flaky, "Flaky"),
    kpi("rate", `${stats.passRate.toFixed(1)}%`, "Pass rate"), kpi("", fmtDur(runMs), "Duration"));

  // ---------- Donut ----------
  (function donut() {
    const parts = [["PASS", stats.passed], ["FAIL", stats.failed], ["ERROR", stats.errors], ["SKIP", stats.skipped]].filter(p => p[1] > 0);
    const r = 60, c = 2 * Math.PI * r; let off = 0;
    const segs = parts.map(([s, n]) => {
      const len = (n / Math.max(stats.total, 1)) * c;
      const seg = `<circle r="${r}" cx="80" cy="80" fill="none" stroke="${COLORS[s]}" stroke-width="22" stroke-dasharray="${len} ${c - len}" stroke-dashoffset="${-off}" transform="rotate(-90 80 80)"/>`;
      off += len; return seg;
    }).join("");
    const svg = `<svg width="160" height="160" viewBox="0 0 160 160"><circle r="${r}" cx="80" cy="80" fill="none" stroke="#eef1f6" stroke-width="22"/>${segs}
      <text x="80" y="78" text-anchor="middle" font-size="26" font-weight="700" fill="#1d2433">${stats.passRate.toFixed(0)}%</text>
      <text x="80" y="98" text-anchor="middle" font-size="11" fill="#6b7385">pass rate</text></svg>`;
    const legend = el("div", { class: "legend" }, ...[["PASS", "Passed", stats.passed], ["FAIL", "Failed", stats.failed], ["ERROR", "Errors", stats.errors], ["SKIP", "Skipped", stats.skipped], ["FLAKY", "Flaky", stats.flaky]]
      .map(([s, l, n]) => el("div", {}, el("span", { class: "dot", style: `background:${s === "FLAKY" ? "var(--flaky)" : COLORS[s]}` }), el("b", {}, n), l)));
    document.getElementById("donut").append(el("div", { class: "donut-wrap", html: svg }), legend);
    document.querySelector(".donut-wrap").append(legend);
  })();

  // ---------- Suites ----------
  const suites = [...new Set(tests.map(t => t.id.className))].map(name => {
    const ts = tests.filter(t => t.id.className === name);
    const count = s => ts.filter(t => t.status === s).length;
    return { name, tests: ts, pass: count("PASS"), fail: count("FAIL"), error: count("ERROR"), skip: count("SKIP"), ms: ts.reduce((a, t) => a + t.durationMs, 0) };
  });
  const bars = document.getElementById("suite-bars");
  suites.forEach(s => {
    const n = s.tests.length;
    const stack = el("div", { class: "stack", title: `${s.pass} passed, ${s.fail} failed, ${s.error} errors, ${s.skip} skipped` },
      ...[["PASS", s.pass], ["FAIL", s.fail], ["ERROR", s.error], ["SKIP", s.skip]].filter(x => x[1]).map(([st, v]) => el("span", { style: `width:${v / n * 100}%;background:${COLORS[st]}` })));
    bars.append(el("div", { class: "sbar", title: s.name, onclick: () => { suiteSel.value = s.name; render(); document.getElementById("tests").scrollIntoView({ behavior: "smooth" }); } },
      el("div", { class: "sname" }, `${simple(s.name)} `, el("span", { class: "tmeta" }, `(${s.pass}/${n})`)), stack, el("div", { class: "sdur" }, fmtDur(s.ms))));
  });

  // ---------- Timeline ----------
  (function timeline() {
    const track = el("div", { class: "tl-track" });
    tests.forEach(t => t.attempts.forEach(a => {
      const left = (a.startedAt - info.startedAt) / runMs * 100, width = Math.max(a.durationMs / runMs * 100, 0.3);
      track.append(el("div", { class: "tl-bar", style: `left:${left}%;width:${width}%;background:${COLORS[a.status]}`,
        title: `${simple(t.id.className)}#${t.id.method} (attempt ${a.index}) — ${a.status} — ${fmtDur(a.durationMs)}`,
        onclick: () => openTest(fullName(t)) }));
    }));
    const axis = el("div", { class: "tl-axis" }, ...[0, 0.25, 0.5, 0.75, 1].map(f => el("span", {}, fmtDur(runMs * f))));
    document.getElementById("timeline").append(el("div", { class: "timeline" }, track, axis));
  })();

  // ---------- Tests ----------
  let statusFilter = "ALL";
  const filters = document.getElementById("filters");
  const counts = { ALL: stats.total, PASS: stats.passed, FAIL: stats.failed, ERROR: stats.errors, SKIP: stats.skipped, FLAKY: stats.flaky };
  ["ALL", "PASS", "FAIL", "ERROR", "SKIP", "FLAKY"].forEach(f => filters.append(
    el("button", { class: `chip ${f === "ALL" ? "active" : ""}`, "data-f": f, onclick: e => {
      statusFilter = f; filters.querySelectorAll(".chip").forEach(c => c.classList.toggle("active", c.dataset.f === f)); render();
    } }, `${f === "ALL" ? "All" : f.charAt(0) + f.slice(1).toLowerCase()} (${counts[f]})`)));
  if (!ok) { statusFilter = "ALL"; }
  const suiteSel = document.getElementById("suite-filter");
  suiteSel.append(el("option", { value: "" }, "All suites"), ...suites.map(s => el("option", { value: s.name }, simple(s.name))));
  suiteSel.addEventListener("change", render);
  const search = document.getElementById("search");
  search.addEventListener("input", render);

  function render() {
    const q = search.value.trim().toLowerCase();
    const host = document.getElementById("tests"); host.replaceChildren();
    suites.filter(s => !suiteSel.value || s.name === suiteSel.value).forEach(s => {
      const shown = s.tests.filter(t =>
        (statusFilter === "ALL" || (statusFilter === "FLAKY" ? t.flaky : t.status === statusFilter)) &&
        (!q || fullName(t).toLowerCase().includes(q)));
      if (!shown.length) return;
      host.append(el("div", { class: "suite-group" }, el("div", { class: "suite-title" }, s.name), ...shown.map(testRow)));
    });
    if (!host.children.length) host.append(el("p", { class: "empty" }, "No tests match the current filters."));
  }

  function testRow(t) {
    const detail = el("div", { class: "detail" });
    const row = el("div", { class: "test", id: `t-${fullName(t)}` },
      el("div", { class: "test-row", onclick: () => { row.classList.toggle("open"); if (!detail.dataset.built) { buildDetail(t, detail); detail.dataset.built = "1"; } } },
        el("span", {}, el("span", { class: `badge ${t.status}` }, t.status)),
        el("span", { class: "tname" }, t.id.method, t.flaky ? el("span", { class: "badge FLAKY" }, "FLAKY") : null,
          ...(t.tags || []).map(g => el("span", { class: "badge TAG" }, g))),
        el("span", { class: "tmeta" }, t.attempts.length > 1 ? `${t.attempts.length} attempts` : ""),
        el("span", { class: "tmeta" }, fmtDur(t.durationMs)),
        el("span", { class: "chev" }, "›")),
      detail);
    return row;
  }

  function buildDetail(t, host) {
    const body = el("div");
    const tabs = el("div", { class: "tabs" });
    t.attempts.forEach((a, i) => tabs.append(el("button", { class: `tab ${i === t.attempts.length - 1 ? "active" : ""}`, onclick: e => {
      tabs.querySelectorAll(".tab").forEach(b => b.classList.remove("active")); e.target.classList.add("active"); show(a);
    } }, `Attempt ${a.index} · ${a.status} · ${fmtDur(a.durationMs)}`)));
    if (t.attempts.length > 1) host.append(tabs);
    host.append(body);
    const cmd = `atr run --test "${simple(t.id.className)}#${t.id.method}"`;
    host.append(el("div", { class: "cmd" }, el("code", {}, cmd),
      el("button", { class: "copy", onclick: e => { navigator.clipboard && navigator.clipboard.writeText(cmd); e.target.textContent = "Copied"; } }, "Copy rerun command")));
    show(t.attempts[t.attempts.length - 1]);

    function show(a) {
      body.replaceChildren();
      body.append(el("div", { class: "tmeta" }, `${t.id.className} · started ${fmtDate(a.startedAt)} · ${fmtDur(a.durationMs)}`));
      if (a.status !== "PASS") body.append(el("div", { class: "error-box" },
        el("div", { class: "msg" }, a.message || `${a.status} without message`),
        a.stackTrace ? el("pre", {}, a.stackTrace) : null));
      const art = a.artifacts || {};
      const media = el("div", { class: "media" });
      const left = el("div", {}, el("div", { class: "section-label" }, "Video"),
        art.video ? el("video", { src: art.video, controls: "", preload: "metadata" }) : el("div", { class: "empty" }, "No video for this attempt."));
      const shots = art.screenshots || [];
      const right = el("div", {}, el("div", { class: "section-label" }, `Screenshots (${shots.length})`),
        shots.length ? el("div", { class: "shots" }, ...shots.map(s => el("figure", {},
          el("img", { src: s, loading: "lazy", alt: s, onclick: () => lightbox(s) }), el("figcaption", {}, s.split("/").pop()))))
          : el("div", { class: "empty" }, "No screenshots."));
      media.append(left, right); body.append(media);

      const log = extras.logcat && art.logcat ? extras.logcat[art.logcat] : null;
      if (log !== null && log !== undefined) {
        const filter = el("input", { type: "search", placeholder: "filter logcat…" });
        const box = el("div", { class: "log" });
        const paint = () => {
          const f = filter.value.toLowerCase();
          box.replaceChildren(...log.split("\n").filter(l => !f || l.toLowerCase().includes(f)).map(l => {
            const m = l.match(/^\S+\s+\S+\s+\d+\s+\d+\s+([VDIWEF])\s/);
            return el("div", { class: m ? m[1] : "" }, l);
          }));
        };
        filter.addEventListener("input", paint); paint();
        body.append(el("div", { class: "section-label log-tools" }, "Logcat (warnings, errors, test runner, app)", filter), box);
      }
      const links = [["Full logcat", art.logcat], ["Instrumentation output", art.instrumentation], ["Video file", art.video]]
        .filter(l => l[1]).map(([n, p]) => el("a", { href: p, target: "_blank" }, n));
      (art.files || []).forEach(f => links.push(el("a", { href: f, target: "_blank" }, f.split("/").pop())));
      if (links.length) body.append(el("div", { class: "section-label" }, "Artifacts"), el("div", { class: "links" }, ...links));
    }
  }

  function openTest(id) {
    statusFilter = "ALL"; suiteSel.value = ""; search.value = "";
    filters.querySelectorAll(".chip").forEach(c => c.classList.toggle("active", c.dataset.f === "ALL"));
    render();
    const row = document.getElementById(`t-${id}`);
    if (row) { if (!row.classList.contains("open")) row.querySelector(".test-row").click(); row.scrollIntoView({ behavior: "smooth", block: "center" }); }
  }

  function lightbox(src) {
    const lb = document.getElementById("lightbox");
    lb.querySelector("img").src = src; lb.querySelector(".lb-caption").textContent = src.split("/").pop(); lb.hidden = false;
  }
  document.getElementById("lightbox").addEventListener("click", e => { e.currentTarget.hidden = true; });
  // Deep link: report.html#t=pkg.Class#method opens that test
  if (location.hash.startsWith("#t=")) setTimeout(() => openTest(decodeURIComponent(location.hash.slice(3))), 0);

  // ---------- Environment ----------
  const envRows = [
    ["Run name", info.name], ["Started", fmtDate(info.startedAt)], ["Finished", fmtDate(info.finishedAt)], ["Duration", fmtDur(runMs)],
    ["Device", `${info.device.manufacturer} ${info.device.model} (${info.device.serial})`], ["Android", `${info.device.androidRelease} (SDK ${info.device.sdk})`],
    ["App package", info.appPackage || "—"], ["Instrumentation", info.instrumentation],
    ["Instrumentation args", Object.entries(info.instrumentationArgs).map(([k, v]) => `${k}=${v}`).join(", ") || "—"],
    ["Selection", info.selection.join(" ") || "all tests"], ["Command", info.command], ["Host", info.host], ["ATR version", info.toolVersion],
  ];
  document.getElementById("env").append(el("table", { class: "env" }, ...envRows.map(([k, v]) => el("tr", {}, el("td", {}, k), el("td", {}, v)))));
  document.getElementById("footer").append(`Generated by ATR ${info.toolVersion} · ${fmtDate(info.finishedAt)}`);

  // Failures first: open the failed filter automatically when something failed.
  if (!ok) filters.querySelector('[data-f="FAIL"]') && (stats.failed ? filters.querySelector('[data-f="FAIL"]').click() : filters.querySelector('[data-f="ERROR"]').click());
  else render();
})();
