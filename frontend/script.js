/* =====================================================================
   HackMatch - Frontend Prototype (HTML, CSS, vanilla JavaScript)

   File layout:
     1. Sample data           (replace with backend data later)
     2. API layer             (TODO: connect to Core Java backend)
     3. Bookmark storage      (localStorage for the prototype)
     4. UI rendering
     5. Event handlers and app start
   ===================================================================== */


/* =====================================================================
   1. SAMPLE DATA
   All records below are FICTIONAL sample data for the prototype.
   They are not real, verified hackathons.
   ===================================================================== */

const SAMPLE_HACKATHONS = [
  {
    id: 1,
    name: "CodeSprint AI Challenge",
    organizer: "Nova Tech Society (Sample)",
    domain: "AI/ML",
    mode: "Online",
    status: "Active",
    prize: 150000,
    deadline: "2026-11-20",
    teamMin: 2,
    teamMax: 4,
    skills: ["Python", "Machine Learning", "Data Analysis"],
    description: "Build an AI model that helps students plan their study schedules.",
    problemStatement: "Design a machine learning system that predicts a student's weak subjects from past test scores and suggests a personalised weekly study plan.",
    eligibility: "Undergraduate students from any college in India.",
    registrationLink: "https://example.com/register/codesprint-ai"
  },
  {
    id: 2,
    name: "WebCraft 2026",
    organizer: "Pixel Developers Club (Sample)",
    domain: "Web Development",
    mode: "Offline",
    status: "Upcoming",
    prize: 75000,
    deadline: "2026-12-05",
    teamMin: 1,
    teamMax: 3,
    skills: ["HTML/CSS", "JavaScript", "UI/UX Design"],
    description: "Create an accessible website for a local non-profit organisation.",
    problemStatement: "Build a responsive, accessible website that lets a small non-profit list volunteer events and accept volunteer sign-ups.",
    eligibility: "Students in 1st to 4th year of any degree. Venue: Pune.",
    registrationLink: "https://example.com/register/webcraft"
  },
  {
    id: 3,
    name: "FinFuture Hack",
    organizer: "Ledger Labs Foundation (Sample)",
    domain: "FinTech",
    mode: "Online",
    status: "Active",
    prize: 200000,
    deadline: "2026-11-15",
    teamMin: 2,
    teamMax: 5,
    skills: ["Java", "SQL", "JavaScript"],
    description: "Design tools that make personal finance simple for young adults.",
    problemStatement: "Build an expense tracker that categorises transactions automatically and shows simple monthly savings insights.",
    eligibility: "Students and recent graduates (within 1 year) in India.",
    registrationLink: "https://example.com/register/finfuture"
  },
  {
    id: 4,
    name: "MediHack India",
    organizer: "CareBridge Health Trust (Sample)",
    domain: "Healthcare",
    mode: "Offline",
    status: "Upcoming",
    prize: 100000,
    deadline: "2027-01-10",
    teamMin: 3,
    teamMax: 4,
    skills: ["Python", "Java", "Data Analysis"],
    description: "Solve real problems faced by rural clinics using technology.",
    problemStatement: "Create a lightweight appointment and patient-record system that works on low-end devices for rural clinics.",
    eligibility: "Engineering and medical students. Venue: Bengaluru.",
    registrationLink: "https://example.com/register/medihack"
  },
  {
    id: 5,
    name: "OpenIdeas Hackfest",
    organizer: "Campus Innovators Network (Sample)",
    domain: "Open Innovation",
    mode: "Online",
    status: "Active",
    prize: 50000,
    deadline: "2026-11-30",
    teamMin: 1,
    teamMax: 4,
    skills: ["Java", "JavaScript", "UI/UX Design"],
    description: "Bring any idea that improves campus life. All tech stacks welcome.",
    problemStatement: "Identify one everyday problem on your college campus and build a working prototype that solves it.",
    eligibility: "Any college student in India. Beginners are encouraged.",
    registrationLink: "https://example.com/register/openideas"
  },
  {
    id: 6,
    name: "DeepVision Datathon",
    organizer: "Insight AI Collective (Sample)",
    domain: "AI/ML",
    mode: "Offline",
    status: "Upcoming",
    prize: 120000,
    deadline: "2026-12-18",
    teamMin: 2,
    teamMax: 3,
    skills: ["Python", "Machine Learning", "SQL"],
    description: "A data-focused challenge on image classification for agriculture.",
    problemStatement: "Train a model that detects common crop diseases from leaf images and explain its predictions to farmers.",
    eligibility: "Undergraduate and postgraduate students. Venue: Hyderabad.",
    registrationLink: "https://example.com/register/deepvision"
  }
];

// SAMPLE recommendation results. In the real project, the Core Java backend
// will calculate these scores and reasons and return them in this same shape.
const SAMPLE_RECOMMENDATIONS = [
  { hackathonId: 1, score: 92, reasons: ["Your Python skill matches the required skills.", "The AI/ML domain matches your interests.", "The team-size requirement fits your preference."] },
  { hackathonId: 5, score: 85, reasons: ["Your Java skill matches the required skills.", "Open Innovation is open to all experience levels."] },
  { hackathonId: 3, score: 78, reasons: ["Your Java and SQL skills match the required skills.", "The prize money is above your preferred minimum."] },
  { hackathonId: 6, score: 64, reasons: ["Your Python skill matches the required skills.", "Team size is slightly smaller than your preference."] }
];

const SKILL_OPTIONS = ["Python", "Java", "JavaScript", "HTML/CSS", "SQL", "Machine Learning", "Data Analysis", "UI/UX Design"];
const DOMAIN_OPTIONS = ["AI/ML", "Web Development", "FinTech", "Healthcare", "Open Innovation"];


/* =====================================================================
   2. API LAYER
   Hackathon listings, recommendations, and bookmarks use the Core Java
   (JDBC + MariaDB) backend when it is available.
   ===================================================================== */

const API_BASE_URL = "http://localhost:8080/api";
const DEMO_STUDENT_ID = 1; // Seeded by database/schema.sql for bookmark demonstrations.
const DEMO_ACCOUNT = Object.freeze({
  name: "Prithvi Singh Kshatriya",
  email: "demo@hackmatch.com",
  password: "Hack123"
});
let usingBackendHackathons = false;
let usingBackendBookmarks = false;
let recommendationSource = "none";

async function getHackathons() {
  try {
    const response = await fetch(API_BASE_URL + "/hackathons");
    if (!response.ok) throw new Error("Hackathon API returned " + response.status + ".");
    const data = await response.json();
    if (!Array.isArray(data)) throw new Error("Hackathon API returned an invalid response.");
    usingBackendHackathons = true;
    return data;
  } catch (error) {
    console.warn("Hackathon API is unavailable; using sample data.", error);
    usingBackendHackathons = false;
    return SAMPLE_HACKATHONS;
  }
}

async function getRecommendations(preferences) {
  try {
    const response = await fetch(API_BASE_URL + "/recommendations", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(preferences)
    });
    if (!response.ok) throw new Error("Recommendations API returned " + response.status + ".");
    const data = await response.json();
    if (!Array.isArray(data)) throw new Error("Recommendations API returned an invalid response.");
    recommendationSource = "backend";
    return data;
  } catch (error) {
    console.warn("Recommendations API is unavailable; using sample recommendations.", error);
    recommendationSource = "fallback";
    return SAMPLE_RECOMMENDATIONS;
  }
}

async function loadBackendBookmarks() {
  try {
    const response = await fetch(API_BASE_URL + "/bookmarks?studentId=" + DEMO_STUDENT_ID);
    if (!response.ok) throw new Error("Bookmarks API returned " + response.status + ".");
    const data = await response.json();
    if (!Array.isArray(data)) throw new Error("Bookmarks API returned an invalid response.");
    bookmarkRecords = data;
    bookmarks = data.map(function (bookmark) { return bookmark.hackathon.id; });
    usingBackendBookmarks = true;
  } catch (error) {
    console.warn("Bookmarks API or Demo Student profile is unavailable; using browser bookmarks.", error);
    bookmarkRecords = [];
    usingBackendBookmarks = false;
  }
}

// Demo authentication intentionally remains frontend-only. The Java API has no auth endpoints.
async function loginUser(credentials) {
  if (credentials.email !== DEMO_ACCOUNT.email || credentials.password !== DEMO_ACCOUNT.password) {
    throw new Error("That email or password does not match the presentation demo account.");
  }
  return { name: DEMO_ACCOUNT.name, email: DEMO_ACCOUNT.email };
}


/* =====================================================================
   3. BOOKMARK STORAGE (MariaDB-backed with browser fallback)
   ===================================================================== */

const STORAGE_KEY = "hackmatch-bookmarks";

function loadStoredBookmarks() {
  try {
    const storedBookmarks = JSON.parse(localStorage.getItem(STORAGE_KEY));
    return Array.isArray(storedBookmarks) ? storedBookmarks : [];
  } catch (error) {
    return [];
  }
}

function saveStoredBookmarks(ids) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(ids));
}

function isBookmarked(id) {
  return bookmarks.includes(id);
}

function switchToBrowserBookmarks() {
  bookmarks = Array.from(new Set(bookmarks.concat(loadStoredBookmarks())));
  bookmarkRecords = [];
  usingBackendBookmarks = false;
  saveStoredBookmarks(bookmarks);
  updateSavedDescription();
}

function toggleBrowserBookmark(id, fallbackNotice) {
  if (isBookmarked(id)) {
    bookmarks = bookmarks.filter(function (savedId) { return savedId !== id; });
  } else {
    bookmarks.push(id);
  }
  saveStoredBookmarks(bookmarks);
  showToast((isBookmarked(id) ? "Saved to your bookmarks" : "Removed from bookmarks") + fallbackNotice);
  renderAll();
}

async function toggleBookmark(id) {
  if (!usingBackendBookmarks) {
    toggleBrowserBookmark(id, "");
    return;
  }

  try {
    const existingBookmark = bookmarkRecords.find(function (bookmark) {
      return bookmark.hackathon.id === id;
    });

    if (existingBookmark) {
      const response = await fetch(API_BASE_URL + "/bookmarks/" + existingBookmark.id, { method: "DELETE" });
      if (!response.ok) throw new Error("Bookmarks API returned " + response.status + ".");
      bookmarkRecords = bookmarkRecords.filter(function (bookmark) { return bookmark.id !== existingBookmark.id; });
      bookmarks = bookmarks.filter(function (savedId) { return savedId !== id; });
      showToast("Removed from your bookmarks");
    } else {
      const response = await fetch(API_BASE_URL + "/bookmarks", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ studentId: DEMO_STUDENT_ID, hackathonId: id })
      });
      if (!response.ok) throw new Error("Bookmarks API returned " + response.status + ".");
      const bookmark = await response.json();
      if (!bookmark || !bookmark.hackathon) throw new Error("Bookmarks API returned an invalid response.");
      bookmarkRecords.push(bookmark);
      bookmarks.push(id);
      showToast("Saved to your bookmarks");
    }
    renderAll();
  } catch (error) {
    console.warn("Bookmarks API is unavailable; switching to browser bookmarks.", error);
    switchToBrowserBookmarks();
    toggleBrowserBookmark(id, " (saved in this browser)");
  }
}

// Demo session (only the public profile, never the password)
const USER_KEY = "hackmatch-user";

function loadUser() {
  try {
    const storedUser = JSON.parse(localStorage.getItem(USER_KEY));
    if (storedUser && storedUser.name === DEMO_ACCOUNT.name && storedUser.email === DEMO_ACCOUNT.email) {
      const session = { name: DEMO_ACCOUNT.name, email: DEMO_ACCOUNT.email };
      localStorage.setItem(USER_KEY, JSON.stringify(session));
      return session;
    }
  } catch (error) {
    // An invalid stored value is treated as no demo session.
  }
  localStorage.removeItem(USER_KEY);
  return null;
}

function setUser(user) {
  currentUser = user ? { name: DEMO_ACCOUNT.name, email: DEMO_ACCOUNT.email } : null;
  if (currentUser) localStorage.setItem(USER_KEY, JSON.stringify(currentUser));
  else localStorage.removeItem(USER_KEY);
  renderUserArea();
}


/* =====================================================================
   4. UI RENDERING
   ===================================================================== */

let hackathons = [];
let bookmarks = loadStoredBookmarks();
let bookmarkRecords = [];
let recommendations = [];
let currentUser = loadUser();

function showToast(message) {
  const toast = document.getElementById("toast");
  toast.textContent = message;
  toast.classList.add("show");
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(function () { toast.classList.remove("show"); }, 2200);
}

function updateDataSourceMessage() {
  const message = usingBackendHackathons
    ? "Local MariaDB demo data is in use. These hackathons are fictional and not verified real events."
    : "Sample fallback data is in use because the local backend is unavailable. These hackathons are fictional and not verified real events.";
  document.getElementById("data-source-message").textContent = message;
  document.getElementById("footer-data-source").textContent = usingBackendHackathons
    ? "frontend prototype · local MariaDB demo data"
    : "frontend prototype · fallback sample data";
}

function updateRecommendationNotice() {
  const title = document.getElementById("recommendation-notice-title");
  const message = document.getElementById("recommendation-notice-message");
  if (recommendationSource === "backend") {
    title.textContent = "Java MatchingService results.";
    message.textContent = "Scores and reasons were calculated from the local MariaDB demo data.";
  } else {
    title.textContent = "Sample fallback recommendations.";
    message.textContent = "The recommendations backend is unavailable, so these scores and reasons were not calculated by Java.";
  }
}

function updateSavedDescription() {
  document.getElementById("saved-description").textContent = usingBackendBookmarks
    ? "Hackathons saved for the local Demo Student profile."
    : "Hackathons saved in this browser because the bookmark service or Demo Student profile is unavailable.";
}

function renderUserArea() {
  const area = document.getElementById("user-area");
  if (currentUser) {
    const initials = currentUser.name.split(" ").map(function (p) { return p[0]; }).join("").slice(0, 2).toUpperCase();
    area.innerHTML = `
      <span class="avatar" aria-hidden="true">${initials}</span>
      <span class="user-name">${currentUser.name}</span>
      <button type="button" class="btn btn-sm btn-ghost" data-action="logout">Log out</button>`;
    document.getElementById("rec-name").textContent = currentUser.name.split(" ")[0];
  } else {
    area.innerHTML = `
      <a href="#login" class="btn btn-sm btn-ghost">Sign in</a>
      <a href="#login" class="btn btn-sm btn-primary">Get started</a>`;
    document.getElementById("rec-name").textContent = "You";
  }
}

function renderStats() {
  const total = hackathons.reduce(function (sum, h) { return sum + h.prize; }, 0);
  document.getElementById("stat-count").textContent = hackathons.length;
  document.getElementById("stat-prize").textContent = "₹" + (total / 100000).toFixed(2) + "L";
}

function formatINR(amount) {
  return "₹" + amount.toLocaleString("en-IN");
}

function formatDate(dateText) {
  return new Date(dateText).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
}

function findHackathon(id) {
  return hackathons.find(function (h) { return h.id === id; });
}

// Shared HTML for the main details of a hackathon (used in every card)
function hackathonInfoHTML(h) {
  const statusClass = h.status === "Active" ? "tag tag-active" : "tag";
  return `
    <div class="card-top">
      <div>
        <h2>${h.name}</h2>
        <p class="organizer muted">by ${h.organizer}</p>
      </div>
    </div>
    <div class="tags">
      <span class="tag tag-domain">${h.domain}</span>
      <span class="tag">${h.mode}</span>
      <span class="${statusClass}">${h.status}</span>
    </div>
    <p class="desc">${h.description}</p>
    <dl class="meta">
      <div><dt>Prize</dt><dd>${formatINR(h.prize)}</dd></div>
      <div><dt>Deadline</dt><dd>${formatDate(h.deadline)}</dd></div>
      <div><dt>Team size</dt><dd>${h.teamMin} - ${h.teamMax} members</dd></div>
    </dl>`;
}

function actionButtonsHTML(h) {
  const saved = isBookmarked(h.id);
  return `
    <div class="card-actions">
      <button class="btn btn-primary" data-action="details" data-id="${h.id}">View Details</button>
      <button class="btn ${saved ? "btn-saved" : ""}" data-action="bookmark" data-id="${h.id}" aria-pressed="${saved}">
        ${saved ? "Saved" : "Bookmark"}
      </button>
    </div>`;
}

function cardHTML(h) {
  return `<article class="card hack-card">${hackathonInfoHTML(h)}${actionButtonsHTML(h)}</article>`;
}

// Explore: apply search and filters, then show matching cards
function renderExplore() {
  const search = document.getElementById("search").value.trim().toLowerCase();
  const domain = document.getElementById("filter-domain").value;
  const mode = document.getElementById("filter-mode").value;
  const minPrize = Number(document.getElementById("filter-prize").value) || 0;
  const status = document.getElementById("filter-status").value;

  const filtered = hackathons.filter(function (h) {
    const searchText = (h.name + " " + h.organizer + " " + h.skills.join(" ")).toLowerCase();
    return searchText.includes(search)
      && (domain === "" || h.domain === domain)
      && (mode === "" || h.mode === mode)
      && (status === "" || h.status === status)
      && h.prize >= minPrize;
  });

  const list = document.getElementById("hackathon-list");
  list.innerHTML = filtered.length
    ? filtered.map(cardHTML).join("")
    : '<div class="empty"><p><strong>No hackathons found.</strong></p><p class="muted">Try changing your search or filters.</p></div>';

  document.getElementById("result-count").textContent =
    "Showing " + filtered.length + " of " + hackathons.length + " hackathons";
}

function renderSaved() {
  const savedHackathons = hackathons.filter(function (h) { return isBookmarked(h.id); });
  document.getElementById("saved-list").innerHTML = savedHackathons.map(cardHTML).join("");
  document.getElementById("saved-empty").hidden = savedHackathons.length > 0;
  document.getElementById("saved-count").textContent = savedHackathons.length;
}

function renderRecommendations() {
  // Sort in descending order of match score
  const sorted = recommendations.slice().sort(function (a, b) { return b.score - a.score; });

  document.getElementById("recommendation-list").innerHTML = sorted.map(function (rec) {
    const h = findHackathon(rec.hackathonId);
    if (!h) return "";
    const reasons = rec.reasons.map(function (r) { return "<li>" + r + "</li>"; }).join("");
    return `
      <article class="card rec-card">
        <div class="score" style="--pct: ${rec.score}" aria-label="Match score ${rec.score} percent">
          <strong>${rec.score}%</strong><span>match</span>
        </div>
        <div class="rec-body">
          ${hackathonInfoHTML(h)}
          <div class="why">
            <h4>Why this matches you</h4>
            <ul>${reasons}</ul>
          </div>
          ${actionButtonsHTML(h)}
        </div>
      </article>`;
  }).join("");
}

function renderAll() {
  renderExplore();
  renderSaved();
  if (recommendations.length) renderRecommendations();
}

function openDetails(id) {
  const h = findHackathon(id);
  const saved = isBookmarked(h.id);
  document.getElementById("modal-content").innerHTML = `
    <div class="modal-head">
      <div>
        <h2 id="modal-title">${h.name}</h2>
        <p class="muted small">Organized by ${h.organizer}</p>
      </div>
      <button class="btn close-btn" data-action="close" aria-label="Close details">&times;</button>
    </div>
    <div class="tags">
      <span class="tag tag-domain">${h.domain}</span>
      <span class="tag">${h.mode}</span>
      <span class="tag">${h.status}</span>
    </div>

    <h3>Overview</h3>
    <p>${h.description}</p>

    <h3>Problem Statement</h3>
    <p>${h.problemStatement}</p>

    <h3>Required Skills</h3>
    <div class="tags">${h.skills.map(function (s) { return '<span class="tag">' + s + "</span>"; }).join("")}</div>

    <h3>Eligibility</h3>
    <p>${h.eligibility}</p>

    <dl class="meta">
      <div><dt>Prize money</dt><dd>${formatINR(h.prize)}</dd></div>
      <div><dt>Registration deadline</dt><dd>${formatDate(h.deadline)}</dd></div>
      <div><dt>Team size</dt><dd>${h.teamMin} - ${h.teamMax} members</dd></div>
    </dl>

    <div class="modal-actions">
      <a class="btn btn-primary" href="${h.registrationLink}" data-action="register" target="_blank" rel="noopener noreferrer">Register (sample link)</a>
      <button class="btn ${saved ? "btn-saved" : ""}" data-action="bookmark" data-id="${h.id}">${saved ? "Saved" : "Bookmark"}</button>
      <button class="btn btn-ghost" data-action="close">Close</button>
    </div>`;

  const modal = document.getElementById("details-modal");
  if (!modal.open) modal.showModal();
}

function renderCheckboxes(containerId, name, options) {
  document.getElementById(containerId).innerHTML = options.map(function (opt) {
    return `<label><input type="checkbox" name="${name}" value="${opt}"> ${opt}</label>`;
  }).join("");
}

// Navigation: show one section at a time, based on the URL hash
function showSection() {
  const current = location.hash.replace("#", "") || "explore";
  document.querySelectorAll(".section").forEach(function (section) {
    section.hidden = section.id !== current;
  });
  document.querySelectorAll(".nav-link").forEach(function (link) {
    link.classList.toggle("active", link.dataset.section === current);
  });
  window.scrollTo(0, 0);
}


/* =====================================================================
   5. EVENT HANDLERS AND APP START
   ===================================================================== */

function getCheckedValues(name) {
  return Array.from(document.querySelectorAll('input[name="' + name + '"]:checked'))
    .map(function (input) { return input.value; });
}

async function handlePreferenceSubmit(event) {
  event.preventDefault();

  const preferences = {
    skills: getCheckedValues("skill"),
    interests: getCheckedValues("interest"),
    experienceLevel: document.getElementById("experience").value,
    minPrize: Number(document.getElementById("pref-prize").value) || 0,
    teamSize: Number(document.getElementById("team-size").value)
  };

  // Basic validation
  const errorBox = document.getElementById("form-error");
  let error = "";
  if (preferences.skills.length === 0) error = "Please select at least one skill.";
  else if (preferences.interests.length === 0) error = "Please select at least one interest.";
  else if (!preferences.experienceLevel) error = "Please select your experience level.";
  else if (!preferences.teamSize) error = "Please select your team size.";
  else if (preferences.minPrize < 0) error = "Minimum prize cannot be negative.";

  errorBox.textContent = error;
  if (error) return;

  recommendations = await getRecommendations(preferences);
  updateRecommendationNotice();
  document.getElementById("payload-preview").textContent = JSON.stringify(preferences, null, 2);
  document.getElementById("recommendation-area").hidden = false;
  renderRecommendations();
  document.getElementById("recommendation-area").scrollIntoView({ behavior: "smooth" });
}

/* ----- Presentation Demo Login ----- */

function value(id) {
  return document.getElementById(id).value.trim();
}

async function handleSignIn(event) {
  event.preventDefault();
  const email = value("signin-email");
  const password = document.getElementById("signin-password").value;
  const errorBox = document.getElementById("signin-error");

  try {
    setUser(await loginUser({ email: email, password: password }));
    event.target.reset();
    errorBox.textContent = "";
    showToast("Welcome, Prithvi!");
    location.hash = "#explore";
  } catch (err) {
    errorBox.textContent = err.message;
  }
}

document.getElementById("signin-form").addEventListener("submit", handleSignIn);

// Show / hide password buttons
document.querySelectorAll(".toggle-pass").forEach(function (button) {
  button.addEventListener("click", function () {
    const input = document.getElementById(button.dataset.target);
    const show = input.type === "password";
    input.type = show ? "text" : "password";
    button.textContent = show ? "Hide" : "Show";
    button.setAttribute("aria-label", show ? "Hide password" : "Show password");
  });
});

// One click handler for all "View Details", "Bookmark", "Close" and "Log out" buttons
document.addEventListener("click", async function (event) {
  const button = event.target.closest("[data-action]");
  if (!button) return;

  if (button.dataset.action === "register" && new URL(button.href).hostname === "example.com") {
    event.preventDefault();
    showToast("This is a demo hackathon. Its registration link is a placeholder and registration is not available.");
    return;
  }

  if (button.dataset.action === "logout") {
    setUser(null);
    showToast("You have been logged out.");
    location.hash = "#explore";
    return;
  }

  const id = Number(button.dataset.id);
  const action = button.dataset.action;
  const modal = document.getElementById("details-modal");

  if (action === "details") openDetails(id);
  if (action === "close") modal.close();
  if (action === "bookmark") {
    await toggleBookmark(id);
    if (modal.open) openDetails(id); // refresh the modal's button text
  }
});

// Close the modal when clicking on the dark backdrop
document.getElementById("details-modal").addEventListener("click", function (event) {
  if (event.target === this) this.close();
});

["search", "filter-domain", "filter-mode", "filter-prize", "filter-status"].forEach(function (id) {
  document.getElementById(id).addEventListener("input", renderExplore);
  document.getElementById(id).addEventListener("change", renderExplore);
});

document.getElementById("reset-filters").addEventListener("click", function () {
  ["search", "filter-domain", "filter-mode", "filter-prize", "filter-status"].forEach(function (id) {
    document.getElementById(id).value = "";
  });
  renderExplore();
});

document.getElementById("preference-form").addEventListener("submit", handlePreferenceSubmit);
window.addEventListener("hashchange", showSection);

async function init() {
  hackathons = await getHackathons();
  if (usingBackendHackathons) await loadBackendBookmarks();
  renderCheckboxes("skill-options", "skill", SKILL_OPTIONS);
  renderCheckboxes("interest-options", "interest", DOMAIN_OPTIONS);
  renderUserArea();
  updateDataSourceMessage();
  updateSavedDescription();
  renderStats();
  showSection();
  renderAll();
}

init();
