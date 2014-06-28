package extractCausal;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import utilities.FileUtilities;

public class runExtractCausal implements Callable<ArrayList<Causal>> {
	private String fileName;
	static boolean flag = false;

	public runExtractCausal(String fileName) {
		super();
		this.fileName = fileName;
	}
	
	@Override
	public ArrayList<Causal> call() throws Exception {
		CausalExtraction ce = new CausalExtraction();
		return ce.getInga(this.fileName);
	}
	
	/**
	 * JSON形式の出力を行う(同期的に)
	 * @param causalList カボリスト
	 */
	public static synchronized void printJson(ArrayList<Causal> causalList) {
		for (Causal causal : causalList) {
			if (!flag) {
				System.out.print(causal.toJson());
				flag = true;
			} else {
				System.out.println(",");
				System.out.print(causal.toJson());
			}
		}
	}
	
	public static void main(String[] args) {
		int threadNum = 2;
		if (args.length != 1 && args.length != 2) {
			System.err.println("ファイル一覧のパスを指定してください。");
		}
		String filePath = args[0];
		if (args.length == 2) {
			threadNum = Integer.parseInt(args[1]);
		}

		// 手がかり表現と指示詞リストの読み込み
		CausalExtraction.setDemonList(FileUtilities.readLines("demonstrative_list.txt"));
		CausalExtraction.setClueList(FileUtilities.readClueList("clue_list.txt"));
		
		String[] files = FileUtilities.readLines(filePath);
		ExecutorService ex = Executors.newFixedThreadPool(threadNum);
		CompletionService<ArrayList<Causal>> completion = new ExecutorCompletionService<ArrayList<Causal>>(ex);
		for (int i = 0; i < files.length; i++) {
			completion.submit(new runExtractCausal(files[i]));
		}
		
		ex.shutdown();
		
		System.out.println("[");
		for (int i = 0; i < files.length; i++) {
			try {
				Future<ArrayList<Causal>> future = completion.take();
				ArrayList<Causal> causalList = future.get();
				runExtractCausal.printJson(causalList);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				continue;
			} catch (ExecutionException e) {
				continue;
			}
		}
		System.out.println();
		System.out.println("]");
	}

}