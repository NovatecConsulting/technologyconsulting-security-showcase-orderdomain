package de.novatec.showcase.order.controller;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import de.novatec.showcase.order.GlobalConstants;
import de.novatec.showcase.order.dto.Address;
import de.novatec.showcase.order.dto.Customer;
import de.novatec.showcase.order.dto.Item;
import de.novatec.showcase.order.dto.ItemQuantityPair;
import de.novatec.showcase.order.dto.ItemQuantityPairs;
import de.novatec.showcase.order.dto.Order;

abstract public class ResourceITBase {

	protected static final String PORT = System.getProperty("http.port");
	protected static final String BASE_URL = "http://localhost:" + PORT + "/orderdomain/";
	protected static final String NON_EXISTING_ID = "1000";
	protected static final String ORDER_URL = BASE_URL + "order/";
	protected static final String ITEM_URL = BASE_URL + "item/";
	protected static final String CUSTOMER_URL = BASE_URL + "customer/";
	protected static Client client;
	protected static Item testItem = null;
	protected static Customer testCustomer = null;
	protected static Order testOrder = null;

	@BeforeClass
	public static void beforeClass() {
		client = ClientBuilder.newClient();
		client.register(JsrJsonpProvider.class);
		client.register(JacksonJsonProvider.class);
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder().build();
		client.register(feature);
		testCustomer = createCustomer();
		testItem = createItem();
		testOrder = createOrder(testCustomer.getId(), testItem);
	}

	@AfterClass
	public static void teardown() {
		client.close();
	}

	public static void assertResponse200(String url, Response response) {
		assertResponse(url, response, Response.Status.OK);
	}

	public static void assertResponse201(String url, Response response) {
		assertResponse(url, response, Response.Status.CREATED);
	}

	public static void assertResponse404(String url, Response response) {
		assertResponse(url, response, Response.Status.NOT_FOUND);
	}

	public static void assertResponse500(String url, Response response) {
		assertResponse(url, response, Response.Status.INTERNAL_SERVER_ERROR);
	}

	public static void assertResponse(String url, Response response, Response.Status status) {
		assertEquals("Incorrect response code from " + url, status.getStatusCode(), response.getStatus());
	}

	protected static Calendar constantDate() {
		Calendar calendar = Calendar.getInstance(Locale.GERMAN);
		calendar.set(Calendar.YEAR, 2019);
		calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
		calendar.set(Calendar.DAY_OF_MONTH, 20);
		return calendar;
	}

	protected static String format(Calendar calendar) {
		return new SimpleDateFormat(GlobalConstants.DATE_FORMAT, Locale.GERMAN).format(calendar.getTime());
	}

	protected static Order createOrder(Integer customerId, Item item) {

		WebTarget target = client.target(ORDER_URL).path(customerId.toString());
		ItemQuantityPairs itemQuantityPairs = new ItemQuantityPairs()
				.setItemQuantityPairs(Arrays.asList(new ItemQuantityPair(item, 1)));
		Builder builder = asAdmin(target.request(MediaType.APPLICATION_JSON));
		Response response = builder.accept(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(itemQuantityPairs));
		assertResponse201(ORDER_URL, response);

		target = client.target(ORDER_URL)
				.path(Integer.valueOf(response.readEntity(JsonObject.class).getInt("id")).toString());
		response = asAdmin(target.request()).get();
		assertResponse200(ORDER_URL, response);

		return response.readEntity(Order.class);
	}

	private static Item createItem() {
		WebTarget target = client.target(ITEM_URL);
		Item item = new Item("name", "description", new BigDecimal(100.0), new BigDecimal(0.0), 1, 0);
		Builder builder = asAdmin(target.request(MediaType.APPLICATION_JSON));
		Response response = builder.accept(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(item));
		assertResponse201(ITEM_URL, response);

		target = client.target(ITEM_URL)
				.path(Integer.valueOf(response.readEntity(JsonObject.class).getString("id")).toString());
		response = asAdmin(target.request()).get();
		assertResponse200(ITEM_URL, response);

		List<Item> items = response.readEntity(new GenericType<List<Item>>() {
		});
		assertEquals("Result should be just one element in an json array!", 1, items.size());
		return items.get(0);
	}

	protected static Customer createCustomer() {
		WebTarget target = client.target(CUSTOMER_URL);
		Customer customer = new Customer("firstname", "lastname", "contact", "GC", new BigDecimal(1000.0),
				constantDate(), new BigDecimal(100.0), new BigDecimal(10.0), null,
				new Address("street1", "street2", "city", "state", "county", "zip", "phone"));
		Builder builder = asAdmin(target.request(MediaType.APPLICATION_JSON));
		Response response = builder.accept(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(customer));
		assertResponse201(CUSTOMER_URL, response);

		target = client.target(CUSTOMER_URL)
				.path(Integer.valueOf(response.readEntity(JsonObject.class).getInt("id")).toString());
		response = asAdmin(target.request(MediaType.APPLICATION_JSON_TYPE)).get();
		assertResponse200(CUSTOMER_URL, response);
		return response.readEntity(Customer.class);
	}

	protected static Builder asAdmin(Builder builder) {
		return asUser(builder, "admin", "adminpwd");
	}

	protected static Builder asTestUser(Builder builder) {
		return asUser(builder, "testuser", "pwd");
	}

	protected static Builder asUser(Builder builder, String userName, String password) {
		return builder.property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, userName)
				.property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, password);
	}

}
