import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import './style.css';
import { CUSTOMERS, SALES_REPS, type Customer } from './customers';
import { optimizeRoute, type GeoPoint, type Visit } from './optimizer';

const app = document.querySelector<HTMLDivElement>('#app')!;

app.innerHTML = `
  <header class="fg-header">
    <h1>Routely — route optimizer demo</h1>
    <p>Nearest-neighbor + 2-opt over the seeded Galicia dataset, running entirely in your browser.</p>
  </header>
  <main class="fg-main">
    <section class="fg-panel">
      <div class="fg-field">
        <label for="rep-select">Rep home base</label>
        <select id="rep-select"></select>
      </div>

      <div class="fg-checkbox-row">
        <input type="checkbox" id="all-active" checked />
        <label for="all-active">All active customers</label>
      </div>

      <div class="fg-field">
        <label for="customer-select">Customers (multi-select)</label>
        <select id="customer-select" multiple disabled></select>
      </div>

      <button class="fg-button" id="compute-button" type="button">Compute route</button>

      <div class="fg-legend">
        <div><span class="fg-legend-swatch naive"></span>Naive (input order)</div>
        <div><span class="fg-legend-swatch optimized"></span>Optimized (NN + 2-opt)</div>
      </div>

      <div class="fg-stat-card" id="stat-card" style="display: none;">
        <h2>Result</h2>
        <div class="fg-stat-row">
          <span>Naive distance</span>
          <span class="fg-stat-value" id="stat-naive"></span>
        </div>
        <div class="fg-stat-row">
          <span>Optimized distance</span>
          <span class="fg-stat-value" id="stat-optimized"></span>
        </div>
        <div class="fg-stat-row fg-stat-highlight">
          <span>Improvement</span>
          <span class="fg-stat-value" id="stat-improvement"></span>
        </div>
        <div class="fg-stat-row">
          <span>2-opt passes</span>
          <span class="fg-stat-value" id="stat-passes"></span>
        </div>
        <div class="fg-stat-row">
          <span>Time window violations</span>
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
    return 'no time window';
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

for (const customer of CUSTOMERS) {
  const marker = L.circleMarker([customer.lat, customer.lng], {
    radius: 6,
    color: '#FFFFFF',
    weight: 1,
    fillColor: PRIORITY_COLOR[customer.priority],
    fillOpacity: customer.active ? 0.9 : 0.35,
  }).addTo(map);
  marker.bindPopup(popupHtml(customer));
}

const homeIcon = (repId: number) =>
  L.divIcon({
    className: 'fg-home-marker',
    html: `<div class="fg-home-pin" title="rep ${repId}"></div>`,
    iconSize: [22, 22],
    iconAnchor: [11, 11],
  });

for (const rep of SALES_REPS) {
  L.marker([rep.lat, rep.lng], { icon: homeIcon(rep.id) })
    .addTo(map)
    .bindPopup(`<div class="fg-popup"><strong>${rep.name}</strong><div class="fg-popup-meta">home base</div></div>`);
}

// -- Controls ---------------------------------------------------------------

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
