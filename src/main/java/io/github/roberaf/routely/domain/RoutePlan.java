package io.github.roberaf.routely.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "route_plan")
public class RoutePlan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sales_rep_id")
	private SalesRep salesRep;

	@Column(name = "plan_date")
	private LocalDate planDate;

	@Column(name = "total_distance_meters")
	private double totalDistanceMeters;

	@Column(name = "naive_distance_meters")
	private double naiveDistanceMeters;

	@Column(name = "computed_at")
	private Instant computedAt;

	@OneToMany(mappedBy = "routePlan", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sequenceIndex ASC")
	private List<RouteStop> stops = new ArrayList<>();

	protected RoutePlan() {
	}

	public RoutePlan(SalesRep salesRep, LocalDate planDate) {
		this.salesRep = salesRep;
		this.planDate = planDate;
	}

	public RouteStop addStop(Customer customer, LocalTime estimatedArrival) {
		RouteStop stop = new RouteStop(this, customer, stops.size(), estimatedArrival);
		stops.add(stop);
		return stop;
	}

	public Long getId() {
		return id;
	}

	public SalesRep getSalesRep() {
		return salesRep;
	}

	public void setSalesRep(SalesRep salesRep) {
		this.salesRep = salesRep;
	}

	public LocalDate getPlanDate() {
		return planDate;
	}

	public void setPlanDate(LocalDate planDate) {
		this.planDate = planDate;
	}

	public double getTotalDistanceMeters() {
		return totalDistanceMeters;
	}

	public void setTotalDistanceMeters(double totalDistanceMeters) {
		this.totalDistanceMeters = totalDistanceMeters;
	}

	public double getNaiveDistanceMeters() {
		return naiveDistanceMeters;
	}

	public void setNaiveDistanceMeters(double naiveDistanceMeters) {
		this.naiveDistanceMeters = naiveDistanceMeters;
	}

	public Instant getComputedAt() {
		return computedAt;
	}

	public void setComputedAt(Instant computedAt) {
		this.computedAt = computedAt;
	}

	public List<RouteStop> getStops() {
		return stops;
	}
}
