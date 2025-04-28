import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// Klasa zadania
class Task implements Runnable {
    private final int id;
    private final String name;
    private String status = "Nie rozpoczęto";
    private String result = null;
    private boolean cancelled = false;
    private boolean working = false;

    public Task(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public synchronized void cancel() {
        this.cancelled = true;
    }

    public synchronized boolean isCancelled() {
        return cancelled;
    }

    public synchronized boolean isFinished() {
        return result != null || cancelled;
    }

    public synchronized String getStatus() {
        return status;
    }

    public synchronized String getResult() {
        return result != null ? result : "Brak wyniku (zadanie nie zakończone)";
    }

    public synchronized boolean isWorking() {
        return working;
    }

    @Override
    public void run() {
        synchronized (this) {
            working = true;
            updateStatus("W trakcie wykonywania");
        }

        Random random = new Random();
        long cumulativeResult = 0; // wynik, który będzie narastał

        for (int i = 1; i <= 15; i++) {
            if (isCancelled()) {
                updateStatus("Anulowane w kroku " + i);
                finishTask();
                return;
            }

            updateStatus("Wykonywanie kroku " + i + "/15");

            // Symulacja pracy - liczenie sumy od 1 do dużej liczby
            int limit = 10_000 + random.nextInt(10_000); // losowo od 10k do 20k
            long partialSum = 0;
            for (int j = 1; j <= limit; j++) {
                partialSum += j;
            }
            cumulativeResult += partialSum;

            try {
                Thread.sleep(5000); // przerwa
            } catch (InterruptedException e) {
                updateStatus("Przerwane przez wyjątek");
                finishTask();
                return;
            }
        }

        synchronized (this) {
            result = "Zadanie " + name + " zakończone. Wynik sumowania: " + cumulativeResult;
            status = "Zakończone";
            working = false;
        }
    }

    private synchronized void updateStatus(String newStatus) {
        this.status = newStatus;
    }

    private synchronized void finishTask() {
        working = false;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

// Zarządca zadań
class TaskManager {
    private final Map<Integer, Task> tasks = new HashMap<>();
    private Thread[] threads;
    private final AtomicInteger taskIdGenerator = new AtomicInteger(1);

    public void startTasks(int count) {
        threads = new Thread[count]; // Inicjalizujemy tablicę
        for (int i = 0; i < count; i++) {
            int id = taskIdGenerator.getAndIncrement();
            Task task = new Task(id, "Zadanie-" + id);
            Thread thread = new Thread(task);
            thread.setName("Thread-" + id);
            tasks.put(id, task);
            threads[i] = thread;
            thread.start();
            System.out.println("Uruchomiono zadanie o ID: " + id);
        }
    }

    public void showTasks() {
        if (tasks.isEmpty()) {
            System.out.println("Brak zadań.");
            return;
        }

        System.out.println("Lista zadań:");
        tasks.values().forEach(task -> {
            System.out.printf("ID: %d, Nazwa: %s, Status: %s, Pracuje: %s%n",
                    task.getId(),
                    task.getName(),
                    task.getStatus(),
                    task.isWorking() ? "Tak" : "Nie"
            );
            if (task.isFinished()) {
                System.out.println("    Wynik: " + task.getResult());
            }
        });
    }

    public void showTaskResult(int id) {
        Task task = tasks.get(id);
        if (task == null) {
            System.out.println("Nie znaleziono zadania o ID: " + id);
            return;
        }

        System.out.printf("Wynik zadania %d (%s): %s%n",
                task.getId(),
                task.getName(),
                task.getResult());
    }

    public void cancelTask(int id) {
        Task task = tasks.get(id);
        if (task == null || task.isFinished()) {
            System.out.println("Nie można anulować zadania " + id + " (już zakończone lub nie istnieje).");
            return;
        }

        task.cancel();

        // Anulowanie wątku po numerze zadania (szukamy w tablicy)
        for (Thread thread : threads) {
            if (thread != null && thread.getName().equals("Thread-" + (task.getId()))) {
                thread.interrupt();
                break;
            }
        }

        System.out.println("Zadanie " + id + " zostało anulowane.");
    }

    public void showActiveThreads() {
        if (threads == null || threads.length == 0) {
            System.out.println("Brak uruchomionych wątków.");
            return;
        }

        System.out.println("Aktywne wątki:");
        for (Thread thread : threads) {
            if (thread != null && thread.isAlive()) {  // Tylko aktywne wątki
                System.out.printf("Nazwa: %s, ID: %d, Alive: %s, State: %s%n",
                        thread.getName(), thread.getId(),
                        thread.isAlive() ? "Tak" : "Nie",
                        thread.getState());
            }
        }
    }

    public void startSingleTask() {
        int id = taskIdGenerator.getAndIncrement();
        Task task = new Task(id, "Zadanie-" + id);
        Thread thread = new Thread(task);
        thread.setName("Thread-" + id);

        // Rozszerzamy tablicę threads
        Thread[] newThreads = Arrays.copyOf(threads, threads.length + 1);
        newThreads[newThreads.length - 1] = thread;
        threads = newThreads;

        tasks.put(id, task);
        thread.start();
        System.out.println("Uruchomiono nowe zadanie o ID: " + id);
    }


}

// Main
public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        TaskManager manager = new TaskManager();

        System.out.print("Ile wątków chcesz uruchomić? ");
        int liczbaWatkow = scanner.nextInt();
        manager.startTasks(liczbaWatkow);


        while (true) {
            System.out.println("\n--- MENU ---");
            System.out.println("1. Pokaż wszystkie zadania");
            System.out.println("2. Pokaż wynik zadania");
            System.out.println("3. Anuluj zadanie");
            System.out.println("4. Pokaż aktywne wątki");
            System.out.println("5. Uruchom nowe zadanie");
            System.out.println("6. Wyjście");

            System.out.print("Wybierz opcję: ");
            int opcja = scanner.nextInt();

            switch (opcja) {
                case 1 -> manager.showTasks();
                case 2 -> {
                    System.out.print("Podaj ID zadania: ");
                    int id = scanner.nextInt();
                    manager.showTaskResult(id);
                }
                case 3 -> {
                    System.out.print("Podaj ID zadania do anulowania: ");
                    int id = scanner.nextInt();
                    manager.cancelTask(id);
                }
                case 4 -> manager.showActiveThreads();
                case 5 -> manager.startSingleTask();
                case 6 -> {
                    System.out.println("Zamykanie programu...");
                    System.exit(0);
                }
                default -> System.out.println("Nieprawidłowa opcja.");
            }
        }


    }
}
