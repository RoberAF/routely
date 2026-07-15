package io.github.roberaf.routely.domain;

import java.time.LocalTime;

import org.locationtech.jts.geom.Point;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer")
public class Customer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	private String address;

	@Column(name = "location", columnDefinition = "geometry(Point,4326)")
	private Point location;

	@Enumerated(EnumType.STRING)
	private CustomerPriority priority;

	@Column(name = "time_window_open")
	private LocalTime timeWindowOpen;

	@Column(name = "time_window_close")
	private LocalTime timeWindowClose;

	private boolean active;

	protected Customer() {
	}

	private Customer(String name, String address, Point location, CustomerPriority priority,
			LocalTime timeWindowOpen, LocalTime timeWindowClose, boolean active) {
		this.name = name;
		this.address = address;
		this.location = location;
		this.priority = priority;
		this.timeWindowOpen = timeWindowOpen;
		this.timeWindowClose = timeWindowClose;
		this.active = active;
	}

	public static Customer create(String name, String address, Point location, CustomerPriority priority,
			LocalTime timeWindowOpen, LocalTime timeWindowClose) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("name must not be blank");
		}
		if (address == null || address.isBlank()) {
			throw new IllegalArgumentException("address must not be blank");
		}
		if (location == null) {
			throw new IllegalArgumentException("location must not be null");
		}
		if (priority == null) {
			throw new IllegalArgumentException("priority must not be null");
		}
		boolean openSet = timeWindowOpen != null;
		boolean closeSet = timeWindowClose != null;
		if (openSet != closeSet) {
			throw new IllegalArgumentException("timeWindowOpen and timeWindowClose must both be set or both be null");
		}
		if (openSet && !timeWindowOpen.isBefore(timeWindowClose)) {
			throw new IllegalArgumentException("timeWindowOpen must be strictly before timeWindowClose");
		}
		return new Customer(name, address, location, priority, timeWindowOpen, timeWindowClose, true);
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Point getLocation() {
		return location;
	}

	public void setLocation(Point location) {
		this.location = location;
	}

	public CustomerPriority getPriority() {
		return priority;
	}

	public void setPriority(CustomerPriority priority) {
		this.priority = priority;
	}

	public LocalTime getTimeWindowOpen() {
		return timeWindowOpen;
	}

	public LocalTime getTimeWindowClose() {
		return timeWindowClose;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public double latitude() {
		return location.getY();
	}

	public double longitude() {
		return location.getX();
	}
}
