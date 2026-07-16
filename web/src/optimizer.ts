// TypeScript port of src/main/java/io/github/roberaf/routely/optimizer/*.java, so the
// web demo can compute the same nearest-neighbor + 2-opt route in the browser without
// a round trip to the API. Kept structurally close to the Java classes (same constants,
// same tie-break rules, same iteration guard) rather than "simplified", so the two stay
// comparable.

export interface GeoPoint {
  lat: number;
  lng: number;
}

export interface Visit {
  customerId: number;
  location: GeoPoint;
  windowOpen: string | null; // "HH:MM", both set or both null
  windowClose: string | null;
}

export interface ScheduledVisit {
  visit: Visit;
  estimatedArrivalMinutes: number;
  withinWindow: boolean;
}

export interface TwoOptResult {
  order: Visit[];
  passesRun: number;
}

export interface OptimizationResult {
  stops: ScheduledVisit[];
  naiveDistanceMeters: number;
  nearestNeighborDistanceMeters: number;
  optimizedDistanceMeters: number;
  twoOptPasses: number;
  timeWindowViolations: number;
  improvementPercent: number;
}

// -- Haversine.java ---------------------------------------------------------

export const EARTH_RADIUS_METERS = 6_371_000.0;

export function haversineMeters(a: GeoPoint, b: GeoPoint): number {
  const lat1 = toRadians(a.lat);
  const lat2 = toRadians(b.lat);
  const dLat = toRadians(b.lat - a.lat);
  const dLng = toRadians(b.lng - a.lng);

  const sinHalfLat = Math.sin(dLat / 2);
  const sinHalfLng = Math.sin(dLng / 2);
  const h = sinHalfLat * sinHalfLat + Math.cos(lat1) * Math.cos(lat2) * sinHalfLng * sinHalfLng;
  const c = 2 * Math.atan2(Math.sqrt(h), Math.sqrt(Math.max(0, 1 - h)));
  return EARTH_RADIUS_METERS * c;
}

function toRadians(degrees: number): number {
  return (degrees * Math.PI) / 180;
}

// -- RouteOptimizer.java constants -------------------------------------------

export const MAX_TWO_OPT_PASSES = 50;
export const DEPARTURE_TIME_MINUTES = 9 * 60; // 09:00
export const AVERAGE_SPEED_MPS = 50_000.0 / 3600.0;
export const SERVICE_TIME_MINUTES = 15;

// -- TourMath.java ------------------------------------------------------------

export function roundTripMeters(home: GeoPoint, order: Visit[]): number {
  if (order.length === 0) {
    return 0;
  }
  let total = 0;
  let current = home;
  for (const visit of order) {
    total += haversineMeters(current, visit.location);
    current = visit.location;
  }
  total += haversineMeters(current, home);
  return total;
}

// -- ScheduleEstimator.java ----------------------------------------------------

interface Arrival {
  effectiveArrivalMinutes: number;
  withinWindow: boolean;
}

function parseTimeToMinutes(hhmm: string): number {
  const [hours, minutes] = hhmm.split(':').map(Number);
  return hours * 60 + minutes;
}

function hasTimeWindow(visit: Visit): boolean {
  return visit.windowOpen !== null && visit.windowClose !== null;
}

function arrive(fromPosition: GeoPoint, fromClockMinutes: number, visit: Visit): Arrival {
  const distanceMeters = haversineMeters(fromPosition, visit.location);
  const travelMinutes = distanceMeters / AVERAGE_SPEED_MPS / 60;
  const rawArrivalMinutes = fromClockMinutes + travelMinutes;

  if (!hasTimeWindow(visit)) {
    return { effectiveArrivalMinutes: rawArrivalMinutes, withinWindow: true };
  }
  const openMinutes = parseTimeToMinutes(visit.windowOpen as string);
  const closeMinutes = parseTimeToMinutes(visit.windowClose as string);
  if (rawArrivalMinutes < openMinutes) {
    return { effectiveArrivalMinutes: openMinutes, withinWindow: true };
  }
  const withinWindow = rawArrivalMinutes <= closeMinutes;
  return { effectiveArrivalMinutes: rawArrivalMinutes, withinWindow };
}

export function schedule(home: GeoPoint, order: Visit[]): ScheduledVisit[] {
  const stops: ScheduledVisit[] = [];
  let currentPosition = home;
  let currentClock = DEPARTURE_TIME_MINUTES;
  for (const visit of order) {
    const arrival = arrive(currentPosition, currentClock, visit);
    stops.push({
      visit,
      estimatedArrivalMinutes: arrival.effectiveArrivalMinutes,
      withinWindow: arrival.withinWindow,
    });
    currentPosition = visit.location;
    currentClock = arrival.effectiveArrivalMinutes + SERVICE_TIME_MINUTES;
  }
  return stops;
}

export function countViolations(stops: ScheduledVisit[]): number {
  return stops.filter((stop) => !stop.withinWindow).length;
}

// -- NearestNeighbor.java -------------------------------------------------------

export function nearestNeighbor(home: GeoPoint, visits: Visit[]): Visit[] {
  const unvisited = [...visits];
  const order: Visit[] = [];

  let currentPosition = home;
  let currentClock = DEPARTURE_TIME_MINUTES;

  while (unvisited.length > 0) {
    const next = pickNext(currentPosition, currentClock, unvisited);
    order.push(next);
    unvisited.splice(unvisited.indexOf(next), 1);

    const arrival = arrive(currentPosition, currentClock, next);
    currentPosition = next.location;
    currentClock = arrival.effectiveArrivalMinutes + SERVICE_TIME_MINUTES;
  }
  return order;
}

function pickNext(currentPosition: GeoPoint, currentClock: number, unvisited: Visit[]): Visit {
  let bestFeasible: Visit | null = null;
  let bestFeasibleDistance = Number.MAX_VALUE;
  let bestOverall: Visit | null = null;
  let bestOverallDistance = Number.MAX_VALUE;

  for (const candidate of unvisited) {
    const distance = haversineMeters(currentPosition, candidate.location);

    if (isCloserOrTieBreaks(distance, candidate, bestOverallDistance, bestOverall)) {
      bestOverall = candidate;
      bestOverallDistance = distance;
    }
    if (
      isFeasible(currentPosition, currentClock, candidate) &&
      isCloserOrTieBreaks(distance, candidate, bestFeasibleDistance, bestFeasible)
    ) {
      bestFeasible = candidate;
      bestFeasibleDistance = distance;
    }
  }
  return bestFeasible ?? (bestOverall as Visit);
}

function isFeasible(currentPosition: GeoPoint, currentClock: number, candidate: Visit): boolean {
  if (!hasTimeWindow(candidate)) {
    return true;
  }
  return arrive(currentPosition, currentClock, candidate).withinWindow;
}

/** Nearer wins; on an exact distance tie the lower customerId wins, for determinism. */
function isCloserOrTieBreaks(distance: number, candidate: Visit, bestDistance: number, best: Visit | null): boolean {
  if (best === null) {
    return true;
  }
  const delta = distance - bestDistance;
  if (delta < -1e-9) {
    return true;
  }
  if (delta > 1e-9) {
    return false;
  }
  return candidate.customerId < best.customerId;
}

// -- TwoOpt.java -----------------------------------------------------------------

const MIN_IMPROVEMENT_METERS = 1e-6;

export function twoOpt(home: GeoPoint, order: Visit[], maxPasses: number): Visit[] {
  return twoOptTracked(home, order, maxPasses).order;
}

/**
 * Same algorithm as {@link twoOpt}, also reporting how many passes actually ran
 * before the loop converged or hit maxPasses, mirroring TwoOpt.improveTracked.
 */
export function twoOptTracked(home: GeoPoint, order: Visit[], maxPasses: number): TwoOptResult {
  let current = [...order];
  const size = current.length;

  let currentDistance = roundTripMeters(home, current);
  let currentViolations = countViolations(schedule(home, current));

  let passesRun = 0;
  for (let pass = 0; pass < maxPasses; pass++) {
    passesRun++;
    let improvedThisPass = false;

    for (let i = 0; i < size - 1; i++) {
      for (let j = i + 1; j < size; j++) {
        const candidate = withSegmentReversed(current, i, j);
        const candidateDistance = roundTripMeters(home, candidate);
        if (candidateDistance >= currentDistance - MIN_IMPROVEMENT_METERS) {
          continue;
        }
        const candidateViolations = countViolations(schedule(home, candidate));
        if (candidateViolations > currentViolations) {
          continue;
        }
        current = candidate;
        currentDistance = candidateDistance;
        currentViolations = candidateViolations;
        improvedThisPass = true;
      }
    }

    if (!improvedThisPass) {
      break;
    }
  }
  return { order: current, passesRun };
}

function withSegmentReversed(order: Visit[], fromIndex: number, toIndex: number): Visit[] {
  const result = [...order];
  let lo = fromIndex;
  let hi = toIndex;
  while (lo < hi) {
    const tmp = result[lo];
    result[lo] = result[hi];
    result[hi] = tmp;
    lo++;
    hi--;
  }
  return result;
}

// -- RouteOptimizer.java -----------------------------------------------------------

export function optimizeRoute(homeBase: GeoPoint, visits: Visit[]): OptimizationResult {
  const naiveDistance = roundTripMeters(homeBase, visits);

  const nearestNeighborOrder = nearestNeighbor(homeBase, visits);
  const nearestNeighborDistance = roundTripMeters(homeBase, nearestNeighborOrder);

  const twoOptResult = twoOptTracked(homeBase, nearestNeighborOrder, MAX_TWO_OPT_PASSES);
  const optimizedDistance = roundTripMeters(homeBase, twoOptResult.order);

  const stops = schedule(homeBase, twoOptResult.order);
  const violations = countViolations(stops);

  const improvementPercent =
    naiveDistance <= 0 ? 0 : ((naiveDistance - optimizedDistance) / naiveDistance) * 100;

  return {
    stops,
    naiveDistanceMeters: naiveDistance,
    nearestNeighborDistanceMeters: nearestNeighborDistance,
    optimizedDistanceMeters: optimizedDistance,
    twoOptPasses: twoOptResult.passesRun,
    timeWindowViolations: violations,
    improvementPercent,
  };
}
