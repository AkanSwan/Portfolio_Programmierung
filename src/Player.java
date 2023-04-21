import java.util.ArrayList;
import java.util.stream.Collectors;

public class Player implements Runnable {

    public static ArrayList<Player> players = new ArrayList<>();
    private static int nextId = 0;
    final String name;
    public int id;
    public PlayerType type;
    public Stock stock;
    public double money = Double.POSITIVE_INFINITY;
    public ArrayList<Activity> activities;

    public Player(String name, PlayerType type, ArrayList<ActivityData> activities) {
        this.id = getNextId();
        this.name = name;
        this.type = type;
        this.activities = activities.stream().map(activityData -> new Activity(this, activityData.type, activityData.product, activityData.minQuantity, activityData.maxQuantity )).collect(Collectors.toCollection(ArrayList::new));
        this.stock = new Stock();

        //add a reference to self
        addToList(this);
    }


    private synchronized int getNextId() {
        return nextId++;
    }

    private synchronized static void addToList(Player player) {
        players.add(player);
    }

    public void playRound() throws InterruptedException {
        prioritizeActivities();
        for(Activity activity : activities)activity.execute();
    }

    public void prioritizeActivities() {
        //TODO:
        // Implementation to be defined
    }

    public void log(String message) {
        Log.getInstance().addMessage(message);
    }

    private void notifyRoundFinished() {
        Synchronizer.allPlayersFinishedRound.countDown();
    }
    @Override
    public void run() {
        // notify loaded
        Synchronizer.allPlayersLoaded.countDown();

        // Wait until the game starts
        try {
            Synchronizer.gameStarted.await();
        } catch (InterruptedException e) {
            log("Player %s left before the game started".formatted(this.name));
            throw new RuntimeException(e);
        }

        // Play the game until finished
        while (!Synchronizer.gameFinished()) {
            try {
                // Wait for a new round
                Synchronizer.newRound.await();

                // Play the round
                log("Player %s starting a new round".formatted(this.name));
                playRound();

                // Mark turn as finished
                notifyRoundFinished();
                log("Player %s finished the round".formatted(this.name));

            } catch (InterruptedException e) {
                log("Player %s left before the game finished".formatted(this.name));
                throw new RuntimeException(e);
            }
        }
    }
}
