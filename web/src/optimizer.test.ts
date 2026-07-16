// Mirrors the fixtures in src/test/java/io/github/roberaf/routely/optimizer/*.java as
// closely as practical, so the TS port is checked against the same known distances and
// the same crafted crossing/edge-case scenarios as the Java optimizer.

import { describe, expect, it } from 'vitest';
import {
  type GeoPoint,
  type Visit,
  MAX_TWO_OPT_PASSES,
  haversineMeters,
  nearestNeighbor,
  optimizeRoute,
  roundTripMeters,
  twoOpt,
} from './optimizer';

function visit(customerId: number, lat: number, lng: number): Visit {
  return { customerId, location: { lat, lng }, windowOpen: null, windowClose: null };
}

function customerIds(order: Visit[]): number[] {
  return order.map((v) => v.customerId);
}

// -- Haversine.java / HaversineTest.java -----------------------------------------

describe('haversineMeters', () => {
  it('is zero from a point to itself', () => {
    const p: GeoPoint = { lat: 43.0121, lng: -7.5559 };
    expect(haversineMeters(p, p)).toBe(0);
  });

  it('is symmetric', () => {
    const a: GeoPoint = { lat: 43.0121, lng: -7.5559 };
    const b: GeoPoint = { lat: 40.4168, lng: -3.7038 };
    expect(haversineMeters(a, b)).toBeCloseTo(haversineMeters(b, a), 9);
  });

  it('one degree of longitude at the equator is ~111,194.93 m', () => {
    const a: GeoPoint = { lat: 0, lng: 0 };
    const b: GeoPoint = { lat: 0, lng: 1 };
    expect(Math.abs(haversineMeters(a, b) - 111_194.93)).toBeLessThan(1.0);
  });

  it('antipodal points are half the earths circumference apart', () => {
    const a: GeoPoint = { lat: 0, lng: 0 };
    const b: GeoPoint = { lat: 0, lng: 180 };
    expect(Math.abs(haversineMeters(a, b) - 20_015_086.8)).toBeLessThan(100.0);
  });

  it('madrid to barcelona is ~505,300 m', () => {
    const madrid: GeoPoint = { lat: 40.4168, lng: -3.7038 };
    const barcelona: GeoPoint = { lat: 41.3874, lng: 2.1686 };
    expect(Math.abs(haversineMeters(madrid, barcelona) - 505_300.0)).toBeLessThan(5_000.0);
  });
});

// -- NearestNeighbor.java / NearestNeighborTest.java -----------------------------

describe('nearestNeighbor', () => {
  const HOME: GeoPoint = { lat: 0, lng: 0 };

  it('picks the nearest unvisited stop each time', () => {
    const far = visit(30, 0, 0.03);
    const near = visit(10, 0, 0.01);
    const middle = visit(20, 0, 0.02);

    const order = nearestNeighbor(HOME, [far, near, middle]);

    expect(customerIds(order)).toEqual([10, 20, 30]);
  });

  it('tie-breaks on exact distance ties by lower customerId', () => {
    const higherId = visit(7, 0, 0.02);
    const lowerId = visit(3, 0, -0.02);

    expect(haversineMeters(HOME, higherId.location)).toBeCloseTo(haversineMeters(HOME, lowerId.location), 9);

    const order = nearestNeighbor(HOME, [higherId, lowerId]);

    expect(customerIds(order)).toEqual([3, 7]);
  });
});

// -- TwoOpt.java / TwoOptTest.java -----------------------------------------------

describe('twoOpt', () => {
  const HOME: GeoPoint = { lat: 0, lng: 0 };

  // A square-ish walk where the naive/crossed order makes the tour's edges cross itself.
  const A = visit(1, 0, 0.2);
  const B = visit(2, 0.15, 0.2);
  const C = visit(3, 0.15, 0);

  it('uncrosses the known crossing fixture', () => {
    const crossed = [A, C, B];
    const initialDistance = roundTripMeters(HOME, crossed);
    expect(Math.abs(initialDistance - 100_075.0)).toBeLessThan(50.0);

    const improved = twoOpt(HOME, crossed, MAX_TWO_OPT_PASSES);
    const improvedDistance = roundTripMeters(HOME, improved);

    expect(improvedDistance).toBeLessThanOrEqual(77_900.0);
    expect(improvedDistance).toBeLessThan(initialDistance);

    const ids = customerIds(improved);
    const isEitherDirectionOfTheSquare =
      JSON.stringify(ids) === JSON.stringify([1, 2, 3]) || JSON.stringify(ids) === JSON.stringify([3, 2, 1]);
    expect(isEitherDirectionOfTheSquare).toBe(true);
  });

  it('cannot improve an already-optimal colinear order', () => {
    const near = visit(10, 0, 0.01);
    const middle = visit(20, 0, 0.02);
    const far = visit(30, 0, 0.03);

    const nnOrder = nearestNeighbor(HOME, [far, near, middle]);
    const nnDistance = roundTripMeters(HOME, nnOrder);

    const improved = twoOpt(HOME, nnOrder, MAX_TWO_OPT_PASSES);
    const improvedDistance = roundTripMeters(HOME, improved);

    expect(improvedDistance).toBe(nnDistance);
  });

  it('the route optimizer improves on a deliberately bad input order', () => {
    const badOrder = [A, C, B];

    const result = optimizeRoute(HOME, badOrder);

    expect(result.naiveDistanceMeters).toBeGreaterThan(result.optimizedDistanceMeters);
    expect(result.improvementPercent).toBeGreaterThan(0);
  });
});

// -- EdgeCasesTest.java -----------------------------------------------------------

describe('optimizeRoute edge cases', () => {
  const HOME: GeoPoint = { lat: 43.0121, lng: -7.5559 };

  it('an empty visit list yields an empty no-op result', () => {
    const result = optimizeRoute(HOME, []);

    expect(result.stops).toHaveLength(0);
    expect(result.naiveDistanceMeters).toBe(0);
    expect(result.nearestNeighborDistanceMeters).toBe(0);
    expect(result.optimizedDistanceMeters).toBe(0);
    expect(result.improvementPercent).toBe(0);
    expect(result.timeWindowViolations).toBe(0);
  });

  it('a single visit is just there and back', () => {
    const onlyStop = visit(1, 43.05, -7.6);
    const expected = 2 * haversineMeters(HOME, onlyStop.location);

    const result = optimizeRoute(HOME, [onlyStop]);

    expect(result.naiveDistanceMeters).toBeCloseTo(expected, 6);
    expect(result.nearestNeighborDistanceMeters).toBeCloseTo(expected, 6);
    expect(result.optimizedDistanceMeters).toBeCloseTo(expected, 6);
    expect(result.stops).toHaveLength(1);
  });

  it('two visits can only get better or stay the same', () => {
    const first = visit(1, 43.05, -7.6);
    const second = visit(2, 42.9, -7.4);

    const result = optimizeRoute(HOME, [first, second]);

    expect(result.optimizedDistanceMeters).toBeLessThanOrEqual(result.naiveDistanceMeters);
    expect(result.stops).toHaveLength(2);
  });
});
