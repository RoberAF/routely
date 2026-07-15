package io.github.roberaf.routely.domain;

import org.locationtech.jts.geom.Point;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sales_rep")
public class SalesRep {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	private String email;

	@Column(name = "home_base", columnDefinition = "geometry(Point,4326)")
	private Point homeBase;

	protected SalesRep() {
	}

	public SalesRep(String name, String email, Point homeBase) {
		this.name = name;
		this.email = email;
		this.homeBase = homeBase;
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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Point getHomeBase() {
		return homeBase;
	}

	public void setHomeBase(Point homeBase) {
		this.homeBase = homeBase;
	}

	public double latitude() {
		return homeBase.getY();
	}

	public double longitude() {
		return homeBase.getX();
	}
}
