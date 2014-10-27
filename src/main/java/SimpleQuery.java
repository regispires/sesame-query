import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class SimpleQuery {

	public static String readFile(String path) throws IOException {
		FileInputStream stream = new FileInputStream(new File(path));
		try {
			FileChannel fc = stream.getChannel();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0,
					fc.size());
			return Charset.defaultCharset().decode(bb).toString();
		} finally {
			stream.close();
		}
	}

	public static long[] execute(String serviceURI, String sparqlFile) {
		long time = -1;
		int i = 0;
		try {
			long startTime = System.currentTimeMillis();
			String queryString;
			queryString = readFile(sparqlFile);

			Repository myRepository = null;

			if (serviceURI == null) {
				// Local execution
				myRepository = new SailRepository(new MemoryStore());
			} else {
				// Remote Execution
				myRepository = new HTTPRepository(serviceURI);
			}
			myRepository.initialize();
			RepositoryConnection con = myRepository.getConnection();
			try {
				TupleQuery tupleQuery = con.prepareTupleQuery(
						QueryLanguage.SPARQL, queryString);
				// FileOutputStream out = new FileOutputStream("results.txt");
				// SPARQLResultsCSVWriter sparqlWriter = new
				// SPARQLResultsCSVWriter(out);
				// tupleQuery.evaluate(sparqlWriter);

				TupleQueryResult result = tupleQuery.evaluate();
				Writer out = new BufferedWriter(new FileWriter("results.txt"));
				if (!result.hasNext()) {
					out.write("No results found.");
				} else {
					System.out.println("Outputting results...");
					for (i = 1; result.hasNext(); i++) {
						MemMon.setMsg("Result #" + i + " - ");
						BindingSet bs = result.next();
						out.write(i + ": " + bs + '\n');
					}
					i--;
				}
				out.close();
				time = System.currentTimeMillis() - startTime;

			} finally {
				con.close();
				myRepository.shutDown();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new long[] { time, i };
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out
					.println("Usage: SimpleQuery <sparqlQueryFile> [<numberOfExecutions>]");
			System.exit(0);
		}
		String serviceURI = null;
		String sparqlFile = args[0];
		int n = 1;
		try {
			n = Integer.parseInt(args[1]);
		} catch (Exception e) {
		}

		long minTime = Long.MAX_VALUE;
		long maxTime = Long.MIN_VALUE;
		long sumTime = 0;

		System.out.println("Execution started.");

		for (int i = 0; i <= n; i++) {
			long[] r = execute(serviceURI, sparqlFile);
			long executionTime = r[0];
			int results = (int) r[1];

			System.out.println(MemMon.memoryInfo());
			MemMon.setMsg("");
			// Do not use the first result
			if (i == 0) {
				System.out.println("Execution #0: Time=" + executionTime
						+ ", Results=" + results + " (Not considered)");
				continue;
			} else {
				System.out.println("Execution #" + i + ": Time="
						+ executionTime + ", Results=" + results);
			}

			if (executionTime < minTime) {
				minTime = executionTime;
			}
			if (executionTime > maxTime) {
				maxTime = executionTime;
			}
			sumTime += executionTime;
		}

		double averageTime = sumTime / (double) n;
		System.out.println(n + " Execution(s): " + "Min=" + minTime + ", Avg="
				+ averageTime + ", Max=" + maxTime);
		System.exit(0);
	}

}
