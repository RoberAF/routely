package io.github.roberaf.routely.domain;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "route_stop")
public class RouteStop {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "route_plan_id")
	private RoutePlan routePlan;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id")
	private Customer customer;

	@Column(name = "sequence_index")
	private int sequenceIndex;

	@Column(name = "estimated_arrival")
	private LocalTime estimatedArrival;

	protected RouteStop() {
	}

	RouteStop(RoutePlan routePlan, Customer customer, int sequenceIndex, LocalTime estimatedArrival) {
		this.routePlan = routePlan;
		this.customer = customer;
		this.sequenceIndex = sequenceIndex;
		this.estimatedArrival = estimatedArrival;
	}

	public Long getId() {
		return id;
	}

	public RoutePlan getRoutePlan() {
		return routePlan;
	}

	public Customer getCustomer() {
		return customer;
	}

	public int getSequenceIndex() {
		return sequenceIndex;
	}

	public LocalTime getEstimatedArrival() {
		return estimatedArrival;
	}

	public void setEstimatedArrival(LocalTime estimatedArrival) {
		this.estimatedArrival = estimatedArrival;
	}
}
