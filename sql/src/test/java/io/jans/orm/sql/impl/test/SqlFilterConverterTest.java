package io.jans.orm.sql.impl.test;

import static org.testng.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.MySQLTemplates;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLTemplates;

import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.impl.SqlFilterConverter;
import io.jans.orm.sql.model.ConvertedExpression;

@SuppressWarnings({ "rawtypes", "unchecked"})
public class SqlFilterConverterTest {

	private SqlFilterConverter simpleConverter;
	private Path<Object> tablePath;
	private Path<Object> docAlias;
	private SimpleExpression<Object> tableAlieasPath;
	private StringPath allPath;
	private SQLTemplates sqlTemplates;
	private Configuration configuration;

	@BeforeClass
	public void init() {
		this.simpleConverter = new SqlFilterConverter(null);
		this.tablePath = ExpressionUtils.path(Object.class, "table");
		this.docAlias = ExpressionUtils.path(Object.class, "doc");
		this.tableAlieasPath = Expressions.as(tablePath, docAlias);
		this.allPath = Expressions.stringPath(docAlias, "*");

		this.sqlTemplates = MySQLTemplates.builder().printSchema().build();
		this.configuration = new Configuration(sqlTemplates);
	}

	@Test
	public void checkEqFilters() throws SearchException {
		// EQ -- String
		Filter filterEq1 = Filter.createEqualityFilter("uid", "test");
		ConvertedExpression expressionEq1 = simpleConverter.convertToSqlFilter(filterEq1, null, null);

		String queryEq1 = toSelectSQL(expressionEq1);
		assertEquals(queryEq1, "select doc.`*` from `table` as doc where doc.uid = 'test'");

		// EQ -- Integer
		Filter filterEq2 = Filter.createEqualityFilter("age", 23);
		ConvertedExpression expressionEq2 = simpleConverter.convertToSqlFilter(filterEq2, null, null);

		String queryEq2 = toSelectSQL(expressionEq2);
		assertEquals(queryEq2, "select doc.`*` from `table` as doc where doc.age = 23");

		// EQ -- Long
		Filter filterEq3 = Filter.createEqualityFilter("age", 23L);
		ConvertedExpression expressionEq3 = simpleConverter.convertToSqlFilter(filterEq3, null, null);

		String queryEq3 = toSelectSQL(expressionEq3);
		assertEquals(queryEq3, "select doc.`*` from `table` as doc where doc.age = 23");

		// EQ -- Date
		Filter filterEq4 = Filter.createEqualityFilter("added", getUtcDateFromMillis(1608130698398L));
		ConvertedExpression expressionEq4 = simpleConverter.convertToSqlFilter(filterEq4, null, null);

		String queryEq4 = toSelectSQL(expressionEq4);
		assertEquals(queryEq4, "select doc.`*` from `table` as doc where doc.added = (timestamp '2020-12-16 14:58:18')");
	}

	@Test
	public void checkMultivaluedEqFilters() throws SearchException {
		// EQ -- String
		Filter filterEq1 = Filter.createEqualityFilter("uid", "test").multiValued();
		ConvertedExpression expressionEq1 = simpleConverter.convertToSqlFilter(filterEq1, null, null);

		String queryEq1 = toSelectSQL(expressionEq1);
		assertEquals(queryEq1, "select doc.`*` from `table` as doc where doc.`uid_.v$` = 'test'");

		// EQ -- Integer
		Filter filterEq2 = Filter.createEqualityFilter("age", 23).multiValued();
		ConvertedExpression expressionEq2 = simpleConverter.convertToSqlFilter(filterEq2, null, null);

		String queryEq2 = toSelectSQL(expressionEq2);
		assertEquals(queryEq2, "select doc.`*` from `table` as doc where doc.`age_.v$` = 23");

		// EQ -- Long
		Filter filterEq3 = Filter.createEqualityFilter("age", 23L).multiValued();
		ConvertedExpression expressionEq3 = simpleConverter.convertToSqlFilter(filterEq3, null, null);

		String queryEq3 = toSelectSQL(expressionEq3);
		assertEquals(queryEq3, "select doc.`*` from `table` as doc where doc.`age_.v$` = 23");

		// EQ -- Date
		Filter filterEq4 = Filter.createEqualityFilter("added", getUtcDateFromMillis(1608130698398L)).multiValued();
		ConvertedExpression expressionEq4 = simpleConverter.convertToSqlFilter(filterEq4, null, null);

		String queryEq4 = toSelectSQL(expressionEq4);
		assertEquals(queryEq4, "select doc.`*` from `table` as doc where doc.`added_.v$` = (timestamp '2020-12-16 14:58:18')");
	}

	@Test
	public void checkLeFilters() throws SearchException {
		// LE -- String
		Filter filterLe1 = Filter.createLessOrEqualFilter("uid", "test");
		ConvertedExpression expressionLe1 = simpleConverter.convertToSqlFilter(filterLe1, null, null);

		String queryLe1 = toSelectSQL(expressionLe1);
		assertEquals(queryLe1, "select doc.`*` from `table` as doc where doc.uid <= 'test'");

		// LE -- Integer
		Filter filterLe2 = Filter.createLessOrEqualFilter("age", 23);
		ConvertedExpression expressionLe2 = simpleConverter.convertToSqlFilter(filterLe2, null, null);

		String queryLe2 = toSelectSQL(expressionLe2);
		assertEquals(queryLe2, "select doc.`*` from `table` as doc where doc.age <= 23");

		// LE -- Long
		Filter filterLe3 = Filter.createLessOrEqualFilter("age", 23L);
		ConvertedExpression expressionLe3 = simpleConverter.convertToSqlFilter(filterLe3, null, null);

		String queryLe3 = toSelectSQL(expressionLe3);
		assertEquals(queryLe3, "select doc.`*` from `table` as doc where doc.age <= 23");

		// LE -- Date
		Filter filterLe4 = Filter.createLessOrEqualFilter("added", getUtcDateFromMillis(1608130698398L));
		ConvertedExpression expressionLe4 = simpleConverter.convertToSqlFilter(filterLe4, null, null);

		String queryLe4 = toSelectSQL(expressionLe4);
		assertEquals(queryLe4, "select doc.`*` from `table` as doc where doc.added <= (timestamp '2020-12-16 14:58:18')");
	}

	@Test
	public void checkMultivaluedLeFilters() throws SearchException {
		// LE -- String
		Filter filterLe1 = Filter.createLessOrEqualFilter("uid", "test").multiValued();
		ConvertedExpression expressionLe1 = simpleConverter.convertToSqlFilter(filterLe1, null, null);

		String queryLe1 = toSelectSQL(expressionLe1);
		assertEquals(queryLe1, "select doc.`*` from `table` as doc where doc.`uid_.v$` <= 'test'");

		// LE -- Integer
		Filter filterLe2 = Filter.createLessOrEqualFilter("age", 23).multiValued();
		ConvertedExpression expressionLe2 = simpleConverter.convertToSqlFilter(filterLe2, null, null);

		String queryLe2 = toSelectSQL(expressionLe2);
		assertEquals(queryLe2, "select doc.`*` from `table` as doc where doc.`age_.v$` <= 23");

		// LE -- Long
		Filter filterLe3 = Filter.createLessOrEqualFilter("age", 23L).multiValued();
		ConvertedExpression expressionLe3 = simpleConverter.convertToSqlFilter(filterLe3, null, null);

		String queryLe3 = toSelectSQL(expressionLe3);
		assertEquals(queryLe3, "select doc.`*` from `table` as doc where doc.`age_.v$` <= 23");

		// LE -- Date
		Filter filterLe4 = Filter.createLessOrEqualFilter("added", getUtcDateFromMillis(1608130698398L)).multiValued();
		ConvertedExpression expressionLe4 = simpleConverter.convertToSqlFilter(filterLe4, null, null);

		String queryLe4 = toSelectSQL(expressionLe4);
		assertEquals(queryLe4, "select doc.`*` from `table` as doc where doc.`added_.v$` <= (timestamp '2020-12-16 14:58:18')");
	}

	@Test
	public void checkGeFilters() throws SearchException {
		// LE -- String
		Filter filterGe1 = Filter.createGreaterOrEqualFilter("uid", "test");
		ConvertedExpression expressionGe1 = simpleConverter.convertToSqlFilter(filterGe1, null, null);

		String queryGe1 = toSelectSQL(expressionGe1);
		assertEquals(queryGe1, "select doc.`*` from `table` as doc where doc.uid >= 'test'");

		// LE -- Integer
		Filter filterGe2 = Filter.createGreaterOrEqualFilter("age", 23);
		ConvertedExpression expressionGe2 = simpleConverter.convertToSqlFilter(filterGe2, null, null);

		String queryGe2 = toSelectSQL(expressionGe2);
		assertEquals(queryGe2, "select doc.`*` from `table` as doc where doc.age >= 23");

		// LE -- Long
		Filter filterGe3 = Filter.createGreaterOrEqualFilter("age", 23L);
		ConvertedExpression expressionGe3 = simpleConverter.convertToSqlFilter(filterGe3, null, null);

		String queryGe3 = toSelectSQL(expressionGe3);
		assertEquals(queryGe3, "select doc.`*` from `table` as doc where doc.age >= 23");

		// LE -- Date
		Filter filterGe4 = Filter.createGreaterOrEqualFilter("added", getUtcDateFromMillis(1608130698398L));
		ConvertedExpression expressionGe4 = simpleConverter.convertToSqlFilter(filterGe4, null, null);

		String queryGe4 = toSelectSQL(expressionGe4);
		assertEquals(queryGe4, "select doc.`*` from `table` as doc where doc.added >= (timestamp '2020-12-16 14:58:18')");
	}

	@Test
	public void checkMultivaluedGeFilters() throws SearchException {
		// LE -- String
		Filter filterGe1 = Filter.createGreaterOrEqualFilter("uid", "test").multiValued();
		ConvertedExpression expressionGe1 = simpleConverter.convertToSqlFilter(filterGe1, null, null);

		String queryGe1 = toSelectSQL(expressionGe1);
		assertEquals(queryGe1, "select doc.`*` from `table` as doc where doc.`uid_.v$` >= 'test'");

		// LE -- Integer
		Filter filterGe2 = Filter.createGreaterOrEqualFilter("age", 23).multiValued();
		ConvertedExpression expressionGe2 = simpleConverter.convertToSqlFilter(filterGe2, null, null);

		String queryGe2 = toSelectSQL(expressionGe2);
		assertEquals(queryGe2, "select doc.`*` from `table` as doc where doc.`age_.v$` >= 23");

		// LE -- Long
		Filter filterGe3 = Filter.createGreaterOrEqualFilter("age", 23L).multiValued();
		ConvertedExpression expressionGe3 = simpleConverter.convertToSqlFilter(filterGe3, null, null);

		String queryGe3 = toSelectSQL(expressionGe3);
		assertEquals(queryGe3, "select doc.`*` from `table` as doc where doc.`age_.v$` >= 23");

		// LE -- Date
		Filter filterGe4 = Filter.createGreaterOrEqualFilter("added", getUtcDateFromMillis(1608130698398L)).multiValued();
		ConvertedExpression expressionGe4 = simpleConverter.convertToSqlFilter(filterGe4, null, null);

		String queryGe4 = toSelectSQL(expressionGe4);
		assertEquals(queryGe4, "select doc.`*` from `table` as doc where doc.`added_.v$` >= (timestamp '2020-12-16 14:58:18')");
	}

	@Test
	public void checkPresenceFilters() throws SearchException {
		// Presence -- String
		Filter filterPresence = Filter.createPresenceFilter("uid");
		ConvertedExpression expressionPresence = simpleConverter.convertToSqlFilter(filterPresence, null, null);

		String queryPresence = toSelectSQL(expressionPresence);
		assertEquals(queryPresence, "select doc.`*` from `table` as doc where exists doc.uid");
	}

	@Test
	public void checkMultivaluedPresenceFilters() throws SearchException {
		// Presence -- String
		Filter filterPresence = Filter.createPresenceFilter("uid").multiValued();
		ConvertedExpression expressionPresence = simpleConverter.convertToSqlFilter(filterPresence, null, null);

		String queryPresence = toSelectSQL(expressionPresence);
		assertEquals(queryPresence, "select doc.`*` from `table` as doc where doc.`uid_.v$` is not null");
	}

	@Test
	public void checkSubFilters() throws SearchException {
		Filter filterSub1 = Filter.createSubstringFilter("uid", null, new String[] { "test" }, null);
		ConvertedExpression expressionSub1 = simpleConverter.convertToSqlFilter(filterSub1, null, null);

		String querySub1 = toSelectSQL(expressionSub1);
		assertEquals(querySub1, "select doc.`*` from `table` as doc where doc.uid like '%test%'");

		Filter filterSub2 = Filter.createSubstringFilter("uid", "a", new String[] { "test" }, null);
		ConvertedExpression expressionSub2 = simpleConverter.convertToSqlFilter(filterSub2, null, null);

		String querySub2 = toSelectSQL(expressionSub2);
		assertEquals(querySub2, "select doc.`*` from `table` as doc where doc.uid like 'a%test%'");

		Filter filterSub3 = Filter.createSubstringFilter("uid", null, new String[] { "test" }, "z");
		ConvertedExpression expressionSub3 = simpleConverter.convertToSqlFilter(filterSub3, null, null);

		String querySub3 = toSelectSQL(expressionSub3);
		assertEquals(querySub3, "select doc.`*` from `table` as doc where doc.uid like '%test%z'");
	}

	@Test
	public void checkMultivaluedSubFilters() throws SearchException {
		Filter filterSub1 = Filter.createSubstringFilter("uid", null, new String[] { "test" }, null).multiValued();
		ConvertedExpression expressionSub1 = simpleConverter.convertToSqlFilter(filterSub1, null, null);

		String querySub1 = toSelectSQL(expressionSub1);
		assertEquals(querySub1, "select doc.`*` from `table` as doc where doc.`uid_.v$` like '%test%'");

		Filter filterSub2 = Filter.createSubstringFilter("uid", "a", new String[] { "test" }, null).multiValued();
		ConvertedExpression expressionSub2 = simpleConverter.convertToSqlFilter(filterSub2, null, null);

		String querySub2 = toSelectSQL(expressionSub2);
		assertEquals(querySub2, "select doc.`*` from `table` as doc where doc.`uid_.v$` like 'a%test%'");

		Filter filterSub3 = Filter.createSubstringFilter("uid", null, new String[] { "test" }, "z").multiValued();
		ConvertedExpression expressionSub3 = simpleConverter.convertToSqlFilter(filterSub3, null, null);

		String querySub3 = toSelectSQL(expressionSub3);
		assertEquals(querySub3, "select doc.`*` from `table` as doc where doc.`uid_.v$` like '%test%z'");
	}

	@Test
	public void checkLowerFilters() throws SearchException {
		Filter userUidFilter1 = Filter.createEqualityFilter(Filter.createLowercaseFilter("uid"), "test");

		ConvertedExpression expressionUserUid1 = simpleConverter.convertToSqlFilter(userUidFilter1, null, null);

		String queryUserUid1 = toSelectSQL(expressionUserUid1);
		assertEquals(queryUserUid1, "select doc.`*` from `table` as doc where lower(doc.uid) = 'test'");
	}

	@Test
	public void checkMultivaluedLowerFilters() throws SearchException {
		Filter userUidFilter = Filter.createEqualityFilter(Filter.createLowercaseFilter("uid"), "test").multiValued();

		ConvertedExpression expressionUserUid = simpleConverter.convertToSqlFilter(userUidFilter, null, null);

		String queryUserUid = toSelectSQL(expressionUserUid);
		assertEquals(queryUserUid, "select doc.`*` from `table` as doc where lower(doc.`uid_.v$`) = 'test'");
	}

	@Test
	public void checkNotFilters() throws SearchException {
		Filter notFilter1 = Filter.createNOTFilter(Filter.createLessOrEqualFilter("age", 23));

		ConvertedExpression expressionNot1 = simpleConverter.convertToSqlFilter(notFilter1, null, null);

		String queryUserUid1 = toSelectSQL(expressionNot1);
		assertEquals(queryUserUid1, "select doc.`*` from `table` as doc where not doc.age <= 23");

		Filter notFilter2 = Filter.createNOTFilter(Filter.createANDFilter(Filter.createLessOrEqualFilter("age", 23), Filter.createGreaterOrEqualFilter("age", 25)));

		ConvertedExpression expressionNot2 = simpleConverter.convertToSqlFilter(notFilter2, null, null);

		String queryUserUid2 = toSelectSQL(expressionNot2);
		assertEquals(queryUserUid2, "select doc.`*` from `table` as doc where not (doc.age <= 23 and doc.age >= 25)");
	}

	@Test
	public void checkAndFilters() throws SearchException {
		Filter filterEq1 = Filter.createEqualityFilter("uid", "test");
		Filter filterPresence1 = Filter.createPresenceFilter("mail");
		Filter filterLe1 = Filter.createLessOrEqualFilter("age", 23);
		Filter filterAnd1 = Filter.createANDFilter(filterPresence1, filterEq1, filterLe1);
		ConvertedExpression expressionAnd1 = simpleConverter.convertToSqlFilter(filterAnd1, null, null);

		String queryAnd1 = toSelectSQL(expressionAnd1);
		assertEquals(queryAnd1, "select doc.`*` from `table` as doc where exists doc.mail and doc.uid = 'test' and doc.age <= 23");
	}

	@Test
	public void checkOrFilters() throws SearchException {
		Filter filterEq1 = Filter.createEqualityFilter("uid", "test");
		Filter filterPresence1 = Filter.createPresenceFilter("mail");
		Filter filterLe1 = Filter.createLessOrEqualFilter("age", 23);
		Filter filterOr1 = Filter.createORFilter(filterPresence1, filterEq1, filterLe1);
		ConvertedExpression expressionAnd1 = simpleConverter.convertToSqlFilter(filterOr1, null, null);

		String queryAnd1 = toSelectSQL(expressionAnd1);
		assertEquals(queryAnd1, "select doc.`*` from `table` as doc where exists doc.mail or doc.uid = 'test' or doc.age <= 23");
	}

	@Test
	public void checkOrJoinFilters() throws SearchException {
		// And with join
		Filter filterEq1 = Filter.createEqualityFilter("uid", "test");
		Filter filterEq2 = Filter.createEqualityFilter("uid", "test2");
		Filter filterEq3 = Filter.createEqualityFilter("uid", "test3");
		Filter filterOr1 = Filter.createORFilter(filterEq1, filterEq2, filterEq3).multiValued(false);
		ConvertedExpression expressionOr1 = simpleConverter.convertToSqlFilter(filterOr1, null, null);

		String queryOr1 = toSelectSQL(expressionOr1);
		assertEquals(queryOr1, "select doc.`*` from `table` as doc where doc.uid in ('test', 'test2', 'test3')");

		Filter filterOr2 = Filter.createORFilter(filterEq1, filterEq2, filterEq3);
		ConvertedExpression expressionOr2 = simpleConverter.convertToSqlFilter(filterOr2, null, null);

		String queryOr2 = toSelectSQL(expressionOr2);
		assertEquals(queryOr2, "select doc.`*` from `table` as doc where doc.uid = 'test' or doc.uid = 'test2' or doc.uid = 'test3'");
	}

	private String toSelectSQL(ConvertedExpression convertedExpression) {
		SQLQuery sqlQuery = (SQLQuery) new SQLQuery(configuration).select(allPath).from(tableAlieasPath)
				.where((Predicate) convertedExpression.expression());
		sqlQuery.setUseLiterals(true);

		String queryStr = sqlQuery.getSQL().getSQL().replace("\n", " ");

		return queryStr;
	}

	private Date getUtcDateFromMillis(long millis) {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.setTimeInMillis(millis);
		calendar.set(Calendar.ZONE_OFFSET, TimeZone.getTimeZone("UTC").getRawOffset());

		Date date = calendar.getTime();

		return date;
	}

}