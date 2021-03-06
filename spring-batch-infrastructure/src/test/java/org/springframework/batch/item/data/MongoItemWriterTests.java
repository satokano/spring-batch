package org.springframework.batch.item.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@SuppressWarnings({"rawtypes", "serial", "unchecked"})
public class MongoItemWriterTests {

	private MongoItemWriter writer;
	@Mock
	private MongoOperations template;
	private PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		writer = new MongoItemWriter();
		writer.setTemplate(template);
		writer.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		writer = new MongoItemWriter();

		try {
			writer.afterPropertiesSet();
			fail("Expected exception was not thrown");
		} catch (IllegalStateException iae) {
		}

		writer.setTemplate(template);
		writer.afterPropertiesSet();
	}

	@Test
	public void testWriteNoTransactionNoCollection() throws Exception {
		List<Object> items = new ArrayList<Object>() {{
			add(new Object());
			add(new Object());
		}};

		writer.write(items);

		verify(template).save(items.get(0));
		verify(template).save(items.get(1));
	}

	@Test
	public void testWriteNoTransactionWithCollection() throws Exception {
		List<Object> items = new ArrayList<Object>() {{
			add(new Object());
			add(new Object());
		}};

		writer.setCollection("collection");

		writer.write(items);

		verify(template).save(items.get(0), "collection");
		verify(template).save(items.get(1), "collection");
	}

	@Test
	public void testWriteNoTransactionNoItems() throws Exception {
		writer.write(null);

		verifyZeroInteractions(template);
	}

	@Test
	public void testWriteTransactionNoCollection() throws Exception {
		final List<Object> items = new ArrayList<Object>() {{
			add(new Object());
			add(new Object());
		}};

		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {

			@Override
			public Object doInTransaction(TransactionStatus status) {
				try {
					writer.write(items);
				} catch (Exception e) {
					fail("An exception was thrown while writing: " + e.getMessage());
				}

				return null;
			}
		});

		verify(template).save(items.get(0));
		verify(template).save(items.get(1));
	}

	@Test
	public void testWriteTransactionWithCollection() throws Exception {
		final List<Object> items = new ArrayList<Object>() {{
			add(new Object());
			add(new Object());
		}};

		writer.setCollection("collection");

		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {

			@Override
			public Object doInTransaction(TransactionStatus status) {
				try {
					writer.write(items);
				} catch (Exception e) {
					fail("An exception was thrown while writing: " + e.getMessage());
				}

				return null;
			}
		});

		verify(template).save(items.get(0), "collection");
		verify(template).save(items.get(1), "collection");
	}

	@Test
	public void testWriteTransactionFails() throws Exception {
		final List<Object> items = new ArrayList<Object>() {{
			add(new Object());
			add(new Object());
		}};

		writer.setCollection("collection");

		try {
			new TransactionTemplate(transactionManager).execute(new TransactionCallback() {

				@Override
				public Object doInTransaction(TransactionStatus status) {
					try {
						writer.write(items);
					} catch (Exception ignore) {
						fail("unexpected exception thrown");
					}
					throw new RuntimeException("force rollback");
				}
			});
		} catch (RuntimeException re) {
			assertEquals(re.getMessage(), "force rollback");
		} catch (Throwable t) {
			fail("Unexpected exception was thrown");
		}

		verifyZeroInteractions(template);
	}

	/**
	 * A pointless use case but validates that the flag is still honored.
	 *
	 * @throws Exception
	 */
	@Test
	public void testWriteTransactionReadOnly() throws Exception {
		final List<Object> items = new ArrayList<Object>() {{
			add(new Object());
			add(new Object());
		}};

		writer.setCollection("collection");

		try {
			TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
			transactionTemplate.setReadOnly(true);
			transactionTemplate.execute(new TransactionCallback() {

				@Override
				public Object doInTransaction(TransactionStatus status) {
					try {
						writer.write(items);
					} catch (Exception ignore) {
						fail("unexpected exception thrown");
					}
					return null;
				}
			});
		} catch (Throwable t) {
			fail("Unexpected exception was thrown");
		}

		verifyZeroInteractions(template);
	}

	@Test
	public void testRemoveNoTransactionNoCollection() throws Exception {
		writer.setDelete(true);
		List<Object> items = new ArrayList<Object>() {{
			add(new Object());
			add(new Object());
		}};

		writer.write(items);

		verify(template).remove(items.get(0));
		verify(template).remove(items.get(1));
	}

	@Test
	public void testRemoveNoTransactionWithCollection() throws Exception {
		writer.setDelete(true);
		List<Object> items = new ArrayList<Object>() {{
			add(new Object());
			add(new Object());
		}};

		writer.setCollection("collection");

		writer.write(items);

		verify(template).remove(items.get(0), "collection");
		verify(template).remove(items.get(1), "collection");
	}
}
