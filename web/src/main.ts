import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import './style.css';
import { CUSTOMERS, SALES_REPS, type Customer, type SalesRep } from './customers';
import { optimizeRoute, type GeoPoint, type Visit } from './optimizer';
import { detectInitialLang, messages, persistLang, type Lang } from './i18n';

const app = document.querySelector<HTMLDivElement>('#app')!;

let lang: Lang = detectInitialLang();

app.innerHTML = `
  <header class="fg-header">
    <div class="fg-header-row">
      <div>
        <h1 id="header-title"></h1>
        <p id="header-subtitle"></p>
      </div>
      <div class="fg-lang-toggle" role="group">
        <button type="button" class="fg-lang-chip" id="lang-en" data-lang="en">EN</button>
        <button type="button" class="fg-lang-chip" id="lang-es" data-lang="es">ES</button>
      </div>
    </div>
  </header>
  <main class="fg-main">
    <section class="fg-panel">
      <div class="fg-field">
        <label for="rep-select" id="rep-label"></label>
        <select id="rep-select"></select>
      </div>

      <div class="fg-checkbox-row">
        <input type="checkbox" id="all-active" checked />
        <label for="all-active" id="all-active-label"></label>
      </div>

      <div class="fg-field">
        <label for="customer-select" id="customers-label"></label>
        <select id="customer-select" multiple disabled></select>
      </div>

      <button class="fg-button" id="compute-button" type="button"></button>

      <div class="fg-legend">
        <div><span class="fg-legend-swatch naive"></span><span id="legend-naive-label"></span></div>
        <div><span class="fg-legend-swatch optimized"></span><span id="legend-optimized-label"></span></div>
      </div>

      <div class="fg-stat-card" id="stat-card" style="display: none;">
        <h2 id="stat-title"></h2>
        <div class="fg-stat-row">
          <span id="stat-naive-label"></span>
          <span class="fg-stat-value" id="stat-naive"></span>
        </div>
        <div class="fg-stat-row">
          <span id="stat-optimized-label"></span>
          <span class="fg-stat-value" id="stat-optimized"></span>
        </div>
        <div class="fg-stat-row fg-stat-highlight">
          <span id="stat-improvement-label"></span>
          <span class="fg-stat-value" id="stat-improvement"></span>
        </div>
        <div class="fg-stat-row">
          <span id="stat-passes-label"></span>
          <span class="fg-stat-value" id="stat-passes"></span>
        </div>
        <div class="fg-stat-row">
          <span id="stat-violations-label"></span>
          <span class="fg-stat-value" id="stat-violations"></span>
        </div>
      </div>
    </section>

    <section id="map"></section>
  </main>
`;

// -- Map setup, centered on Galicia --------------------------------------------

const map = L.map('map').setView([42.75, -7.6], 8);

L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
  attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
  maxZoom: 18,
}).addTo(map);

const PRIORITY_COLOR: Record<Customer['priority'], string> = {
  KEY: '#D9642A',
  NORMAL: '#2A7E74',
  LOW: '#8F877B',
};

function priorityBadgeClass(priority: Customer['priority']): string {
  return priority.toLowerCase();
}

function formatWindow(customer: Customer): string {
  if (!customer.windowOpen || !customer.windowClose) {
    return messages[lang].popupNoTimeWindow;
  }
  return `${customer.windowOpen}–${customer.windowClose}`;
}

function popupHtml(customer: Customer): string {
  return `
    <div class="fg-popup">
      <strong>${customer.name}</strong><br />
      <span class="fg-badge ${priorityBadgeClass(customer.priority)}">${customer.priority}</span>
      <div class="fg-popup-meta">${customer.address}</div>
      <div class="fg-popup-meta">${formatWindow(customer)}</div>
    </div>
  `;
}

function repPopupHtml(rep: SalesRep): string {
  return `<div class="fg-popup"><strong>${rep.name}</strong><div class="fg-popup-meta">${messages[lang].popupHomeBase}</div></div>`;
}

const customerMarkers = new Map<number, L.CircleMarker>();
for (const customer of CUSTOMERS) {
  const marker = L.circleMarker([customer.lat, customer.lng], {
    radius: 6,
    color: '#FFFFFF',
    weight: 1,
    fillColor: PRIORITY_COLOR[customer.priority],
    fillOpacity: customer.active ? 0.9 : 0.35,
  }).addTo(map);
  marker.bindPopup(popupHtml(customer));
  customerMarkers.set(customer.id, marker);
}

const homeIcon = (repId: number) =>
  L.divIcon({
    className: 'fg-home-marker',
    html: `<div class="fg-home-pin" title="rep ${repId}"></div>`,
    iconSize: [22, 22],
    iconAnchor: [11, 11],
  });

const repMarkers = new Map<number, L.Marker>();
for (const rep of SALES_REPS) {
  const marker = L.marker([rep.lat, rep.lng], { icon: homeIcon(rep.id) })
    .addTo(map)
    .bindPopup(repPopupHtml(rep));
  repMarkers.set(rep.id, marker);
}

function refreshPopups(): void {
  for (const customer of CUSTOMERS) {
    customerMarkers.get(customer.id)?.setPopupContent(popupHtml(customer));
  }
  for (const rep of SALES_REPS) {
    repMarkers.get(rep.id)?.setPopupContent(repPopupHtml(rep));
  }
}

// -- Controls ---------------------------------------------------------------

const headerTitle = document.querySelector<HTMLHeadingElement>('#header-title')!;
const headerSubtitle = document.querySelector<HTMLParagraphElement>('#header-subtitle')!;
const repLabel = document.querySelector<HTMLLabelElement>('#rep-label')!;
const allActiveLabel = document.querySelector<HTMLLabelElement>('#all-active-label')!;
const customersLabel = document.querySelector<HTMLLabelElement>('#customers-label')!;
const legendNaiveLabel = document.querySelector<HTMLSpanElement>('#legend-naive-label')!;
const legendOptimizedLabel = document.querySelector<HTMLSpanElement>('#legend-optimized-label')!;
const statTitle = document.querySelector<HTMLHeadingElement>('#stat-title')!;
const statNaiveLabel = document.querySelector<HTMLSpanElement>('#stat-naive-label')!;
const statOptimizedLabel = document.querySelector<HTMLSpanElement>('#stat-optimized-label')!;
const statImprovementLabel = document.querySelector<HTMLSpanElement>('#stat-improvement-label')!;
const statPassesLabel = document.querySelector<HTMLSpanElement>('#stat-passes-label')!;
const statViolationsLabel = document.querySelector<HTMLSpanElement>('#stat-violations-label')!;
const langEnButton = document.querySelector<HTMLButtonElement>('#lang-en')!;
const langEsButton = document.querySelector<HTMLButtonElement>('#lang-es')!;
const langToggle = document.querySelector<HTMLDivElement>('.fg-lang-toggle')!;

const repSelect = document.querySelector<HTMLSelectElement>('#rep-select')!;
const allActiveCheckbox = document.querySelector<HTMLInputElement>('#all-active')!;
const customerSelect = document.querySelector<HTMLSelectElement>('#customer-select')!;
const computeButton = document.querySelector<HTMLButtonElement>('#compute-button')!;
const statCard = document.querySelector<HTMLDivElement>('#stat-card')!;
const statNaive = document.querySelector<HTMLSpanElement>('#stat-naive')!;
const statOptimized = document.querySelector<HTMLSpanElement>('#stat-optimized')!;
const statImprovement = document.querySelector<HTMLSpanElement>('#stat-improvement')!;
const statPasses = document.querySelector<HTMLSpanElement>('#stat-passes')!;
const statViolations = document.querySelector<HTMLSpanElement>('#stat-violations')!;

function applyTranslations(): void {
  const m = messages[lang];
  document.title = m.pageTitle;
  document.documentElement.lang = lang;
  headerTitle.textContent = m.headerTitle;
  headerSubtitle.textContent = m.headerSubtitle;
  repLabel.textContent = m.repLabel;
  allActiveLabel.textContent = m.allActiveLabel;
  customersLabel.textContent = m.customersLabel;
  computeButton.textContent = m.computeButton;
  legendNaiveLabel.textContent = m.legendNaive;
  legendOptimizedLabel.textContent = m.legendOptimized;
  statTitle.textContent = m.statTitle;
  statNaiveLabel.textContent = m.statNaiveLabel;
  statOptimizedLabel.textContent = m.statOptimizedLabel;
  statImprovementLabel.textContent = m.statImprovementLabel;
  statPassesLabel.textContent = m.statPassesLabel;
  statViolationsLabel.textContent = m.statViolationsLabel;
  langToggle.setAttribute('aria-label', m.langToggleAriaLabel);
  langEnButton.classList.toggle('active', lang === 'en');
  langEnButton.setAttribute('aria-pressed', String(lang === 'en'));
  langEsButton.classList.toggle('active', lang === 'es');
  langEsButton.setAttribute('aria-pressed', String(lang === 'es'));
}

function setLang(next: Lang): void {
  if (lang === next) {
    return;
  }
  lang = next;
  persistLang(lang);
  applyTranslations();
  refreshPopups();
}

langEnButton.addEventListener('click', () => setLang('en'));
langEsButton.addEventListener('click', () => setLang('es'));

applyTranslations();

for (const rep of SALES_REPS) {
  const option = document.createElement('option');
  option.value = String(rep.id);
  option.textContent = rep.name;
  repSelect.appendChild(option);
}

const activeCustomers = CUSTOMERS.filter((c) => c.active);
for (const customer of activeCustomers) {
  const option = document.createElement('option');
  option.value = String(customer.id);
  option.textContent = `${customer.name} (${customer.priority})`;
  customerSelect.appendChild(option);
}

allActiveCheckbox.addEventListener('change', () => {
  customerSelect.disabled = allActiveCheckbox.checked;
});

function selectedVisits(): Visit[] {
  const customers = allActiveCheckbox.checked
    ? activeCustomers
    : Array.from(customerSelect.selectedOptions).map(
        (option) => activeCustomers.find((c) => c.id === Number(option.value))!,
      );
  return customers.map(toVisit);
}

function toVisit(customer: Customer): Visit {
  return {
    customerId: customer.id,
    location: { lat: customer.lat, lng: customer.lng },
    windowOpen: customer.windowOpen,
    windowClose: customer.windowClose,
  };
}

function formatMeters(meters: number): string {
  return `${Math.round(meters).toLocaleString('en-US')} m`;
}

// -- Route layers, redrawn on every compute -----------------------------------

let naiveLine: L.Polyline | null = null;
let optimizedLine: L.Polyline | null = null;

function toLatLngs(home: GeoPoint, order: Visit[]): L.LatLngExpression[] {
  const points: L.LatLngExpression[] = [[home.lat, home.lng]];
  for (const visit of order) {
    points.push([visit.location.lat, visit.location.lng]);
  }
  points.push([home.lat, home.lng]);
  return points;
}

computeButton.addEventListener('click', () => {
  const repId = Number(repSelect.value);
  const rep = SALES_REPS.find((r) => r.id === repId);
  if (!rep) {
    return;
  }
  const home: GeoPoint = { lat: rep.lat, lng: rep.lng };
  const visits = selectedVisits();

  const result = optimizeRoute(home, visits);

  if (naiveLine) {
    naiveLine.remove();
  }
  if (optimizedLine) {
    optimizedLine.remove();
  }

  naiveLine = L.polyline(toLatLngs(home, visits), {
    color: '#8F877B',
    weight: 3,
    dashArray: '6, 8',
  }).addTo(map);

  const optimizedOrder = result.stops.map((stop) => stop.visit);
  optimizedLine = L.polyline(toLatLngs(home, optimizedOrder), {
    color: '#D9642A',
    weight: 4,
  }).addTo(map);

  if (visits.length > 0) {
    map.fitBounds(L.latLngBounds(toLatLngs(home, visits)), { padding: [24, 24] });
  }

  statCard.style.display = 'flex';
  statNaive.textContent = formatMeters(result.naiveDistanceMeters);
  statOptimized.textContent = formatMeters(result.optimizedDistanceMeters);
  statImprovement.textContent = `${result.improvementPercent.toFixed(1)}%`;
  statPasses.textContent = String(result.twoOptPasses);
  statViolations.textContent = String(result.timeWindowViolations);
});
