package io.github.roberaf.routely.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Boots the full context against a real PostGIS container and checks that
 * Flyway actually ran both migrations and left the seed data in the shape
 * the rest of the API relies on.
 */
@Tag("integration")
class FlywayMigrationIT extends PostgisTestSupport {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void bothMigrationsRanSuccessfully() {
		List<Boolean> successFlags = jdbcTemplate.queryForList(
				"SELECT success FROM flyway_schema_history ORDER BY installed_rank", Boolean.class);

		assertThat(successFlags).hasSize(2);
		assertThat(successFlags).allMatch(Boolean::booleanValue);
	}

	@Test
	void postgisExtensionIsInstalled() {
		String version = jdbcTemplate.queryForObject("SELECT postgis_version()", String.class);

		assertThat(version).isNotBlank();
	}

	@Test
	void seedDataRowCountsMatchTheGaliciaFixture() {
		assertThat(rowCount("sales_rep")).isEqualTo(3);
		assertThat(rowCount("app_user")).isEqualTo(4);
		assertThat(rowCount("customer")).isEqualTo(40);
	}

	@Test
	void customerLocationHasAGistIndex() {
		List<String> indexNames = jdbcTemplate.queryForList(
				"SELECT indexname FROM pg_indexes WHERE tablename = 'customer'", String.class);

		assertThat(indexNames).contains("customer_location_gix");
	}

	private long rowCount(String table) {
		Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
		assertThat(count).isNotNull();
		return count;
	}
}
