package tickets;

import genericqueue.GenericQueue;
import genericqueue.GenericQueueImpl;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TicketApp {

    private static final int CAPACITY_DEFAULT = 10;
    private static final long DEADLINE_SECONDS = 60;

    public record JoinBody(String name, int quantity) {}
    public record AdjustBody(String name, int quantity) {}
    public record LeaveBody(String name) {}
    public record PurchaseBody(String name, int quantity) {}
    public record RenameBody(String name, String newName) {}
    public record AdminInsertBody(String name, String after, int quantity) {}
    public record WaiterInfo(int quantity, long joinedMs) {}

    // shopping-center page
    private static class Customer {
        final String name;
        final int quantity;       // reserved ticket count
        final long admittedMs;     // when they entered the shopping center milliseconds

        Customer(String name, int quantity) {
            this.name       = name;
            this.quantity   = quantity;
            this.admittedMs = System.currentTimeMillis();
        }

        long secondsLeft() {
            long elapsed = (System.currentTimeMillis() - admittedMs) / 1000;
            return Math.max(0, DEADLINE_SECONDS - elapsed);
        }

        boolean expired() {
            return secondsLeft() == 0;
        }
    }

    private final int totalCapacity;
    // waiting queue: name -> reserved quantity
    private final GenericQueue<WaiterInfo> waitingQueue = new GenericQueueImpl<>();
    // people currently in the shopping center
    private final Map<String, Customer> customers = new LinkedHashMap<>();
    // past purchases: name -> list of formatted purchase strings
    private final Map<String, List<String>> purchases = new HashMap<>();

    public TicketApp(int totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public static void main(String[] args) {
        TicketApp app = new TicketApp(CAPACITY_DEFAULT);
        Javalin server = Javalin.create(config -> {
            config.staticFiles.add("/static");
            config.routes.post("/api/join", app::handleJoin);
            config.routes.get( "/api/status", app::handleStatus);
            config.routes.post("/api/adjust", app::handleAdjust);
            config.routes.post("/api/leave", app::handleLeave);
            config.routes.post("/api/purchase", app::handlePurchase);
            config.routes.get( "/api/profile", app::handleProfile);
            config.routes.post("/api/rename", app::handleRename);
            config.routes.get( "/api/admin", app::handleAdmin);
            config.routes.post("/api/admin/insert", app::handleAdminInsert);
        });
        server.start(7070);
        System.out.println("Ticket app: open http://localhost:7070/");
    }

    // kick anyone who timed out and invite those waiting in
    private void updateCustomers() {
        removeExpired();
        admitInWaiting();
    }

    private void handleJoin(Context ctx) {
        updateCustomers();
        JoinBody body;
        try { body = ctx.bodyAsClass(JoinBody.class); }
        catch (Exception e) { handleError(ctx, 400, "Invalid request body."); return; }

        String name = body.name() == null ? "" : body.name().trim();
        int quantity = body.quantity();

        if (name.isEmpty()) {
            handleError(ctx, 400, "Name is required."); return;
        }
        if (quantity < 1 || quantity > totalCapacity) {
            handleError(ctx, 400, "Quantity must be between 1 and " + totalCapacity + "."); return;
        }
        if (customers.containsKey(name)) {
            handleError(ctx, 400, name + " is already in the system."); return;
        }
        if (waitingQueue.contains(name)) {
            handleError(ctx, 400, name + " is already in the waiting queue."); return;
        }

        // new customers only skip the queue when nobody else waiting
        if (waitingQueue.isEmpty() && usedCapacity() + quantity <= totalCapacity) {
            customers.put(name, new Customer(name, quantity));
            sendShoppingStatus(ctx, customers.get(name));
        } else {
            waitingQueue.enqueue(name, new WaiterInfo(quantity, System.currentTimeMillis()));
            sendWaitingStatus(ctx, name, quantity, 0);
        }
    }

    private void handleStatus(Context ctx) {
        updateCustomers();
        String name = ctx.queryParam("name");
        if (name == null || name.isBlank()) {
            handleError(ctx, 400, "Name is required."); return;
        }
        name = name.trim();

        if (customers.containsKey(name)) {
            sendShoppingStatus(ctx, customers.get(name));
        } else if (waitingQueue.contains(name)) {
            WaiterInfo info = waitingQueue.getEntry(name).value();
            long waitSeconds = (System.currentTimeMillis() - info.joinedMs()) / 1000;
            sendWaitingStatus(ctx, name, info.quantity(), waitSeconds);
        } else {
            handleError(ctx, 400, name + " is not in the queue or shopping center.");
        }
    }

    private void handleAdjust(Context ctx) {
        updateCustomers();
        AdjustBody body;
        try { body = ctx.bodyAsClass(AdjustBody.class); }
        catch (Exception e) { handleError(ctx, 400, "Invalid request body."); return; }

        String name = body.name() == null ? "" : body.name().trim();
        int quantity = body.quantity();

        if (name.isEmpty()) {
            handleError(ctx, 400, "Name is required."); return;
        }
        if (quantity < 1 || quantity > totalCapacity) {
            handleError(ctx, 400, "Quantity must be between 1 and " + totalCapacity + "."); return;
        }
        if (!waitingQueue.contains(name)) {
            handleError(ctx, 400, name + " is not in the waiting queue."); return;
        }

        long originalJoinedMs = waitingQueue.getEntry(name).value().joinedMs();
        waitingQueue.updateValue(name, new WaiterInfo(quantity, originalJoinedMs));
        updateCustomers();

        if (customers.containsKey(name)) {
            Customer s = customers.get(name);
            ctx.json(Map.of(
                    "status", "shopping",
                    "quantity", s.quantity,
                    "secondsLeft", s.secondsLeft() ));
        } else {
            ctx.json(Map.of(
                    "status", "waiting",
                    "position", waitingQueue.indexOfName(name) + 1,
                    "waitSeconds", 0,
                    "quantity", quantity ));
        }
    }

    private void handleLeave(Context ctx) {
        updateCustomers();
        LeaveBody body;
        try { body = ctx.bodyAsClass(LeaveBody.class); }
        catch (Exception e) { handleError(ctx, 400, "Invalid request body."); return; }

        String name = body.name() == null ? "" : body.name().trim();
        if (name.isEmpty()) {
            handleError(ctx, 400, "Name is required."); return;
        }

        if (customers.containsKey(name)) {
            customers.remove(name);
            updateCustomers();
            ctx.json(Map.of("status", "left"));
        } else if (waitingQueue.contains(name)) {
            waitingQueue.remove(name);
            updateCustomers();
            ctx.json(Map.of("status", "left"));
        } else {
            handleError(ctx, 400, name + " is not in the queue or shopping center."); return;
        }
    }

    private void handlePurchase(Context ctx) {
        updateCustomers();
        PurchaseBody body;
        try { body = ctx.bodyAsClass(PurchaseBody.class); }
        catch (Exception e) { handleError(ctx, 400, "Invalid request body."); return; }

        String name = body.name() == null ? "" : body.name().trim();
        int quantity = body.quantity();

        if (name.isEmpty()) {
            handleError(ctx, 400, "Name is required."); return;
        }
        if (!customers.containsKey(name)) {
            handleError(ctx, 400, name + " is not in the shopping center."); return;
        }

        Customer s = customers.get(name);
        if (quantity < 1 || quantity > s.quantity) {
            handleError(ctx, 400,"Quantity must be between 1 and " + s.quantity + "."); return;
        }

        // Save purchase so it can appear on the profile page.
        savePurchase(name, quantity);

        customers.remove(name);
        updateCustomers();
        ctx.json(Map.of("status", "purchased", "quantity", quantity ));
    }

    private void handleProfile(Context ctx) {
        updateCustomers();
        String name = ctx.queryParam("name");
        if (name == null || name.isBlank()) {
            handleError(ctx, 400, "Name is required."); return;
        }
        name = name.trim();

        List<String> userPurchases = purchases.getOrDefault(name, List.of());
        ctx.json(Map.of(
                "name", name,
                "purchases", userPurchases ));
    }

    private void handleRename(Context ctx) {
        updateCustomers();
        RenameBody body;
        try { body = ctx.bodyAsClass(RenameBody.class); }
        catch (Exception e) { handleError(ctx, 400, "Invalid request body."); return; }

        String oldName = body.name() == null ? "" : body.name().trim();
        String newName = body.newName() == null ? "" : body.newName().trim();

        if (oldName.isEmpty() || newName.isEmpty()) {
            handleError(ctx, 400, "Both name and new name are required."); return;
        }
        if (oldName.equals(newName)) {
            ctx.json(Map.of("name", newName)); return;
        }
        // check if name already exists in queue
        if (waitingQueue.contains(newName) || customers.containsKey(newName) || purchases.containsKey(newName)) {
            handleError(ctx, 400, newName + " is already being used.");
            return;
        }

        // rename in queue if present
        if (waitingQueue.contains(oldName)) {
            waitingQueue.rename(oldName, newName);
        }

        // rename in shopping center if present
        if (customers.containsKey(oldName)) {
            Customer s = customers.remove(oldName);
            customers.put(newName, new Customer(newName, s.quantity));
        }

        // rename purchase history
        if (purchases.containsKey(oldName)) {
            purchases.put(newName, purchases.remove(oldName));
        }

        ctx.json(Map.of("name", newName));
    }

    private void handleAdmin(Context ctx) {
        updateCustomers();

        GenericQueue.Entry<WaiterInfo> front = waitingQueue.peek();
        String nextUp = front == null ? null
                : front.name() + " (" + front.value().quantity() + " ticket" + (front.value().quantity() == 1 ? "" : "s") + ")";

        int queueTickets = waitingQueue.entries().stream().mapToInt(e -> e.value().quantity()).sum();

        ctx.json(Map.of(
                "totalCapacity", totalCapacity,
                "usedCapacity", usedCapacity(),
                "queueLength", waitingQueue.size(),
                "queueTickets", queueTickets,
                "nextUp", nextUp == null ? "" : nextUp ));
    }

    private void handleAdminInsert(Context ctx) {
        updateCustomers();
        AdminInsertBody body;
        try { body = ctx.bodyAsClass(AdminInsertBody.class); }
        catch (Exception e) { ctx.status(400).json(Map.of("error", "Invalid request body.")); return; }

        String name = body.name()  == null ? "" : body.name().trim();
        String after = body.after() == null ? "" : body.after().trim();
        int quantity = body.quantity();

        if (name.isEmpty()) {
            handleError(ctx, 400, "Name is required."); return;
        }
        if (quantity < 1 || quantity > totalCapacity) {
            handleError(ctx, 400, "Quantity must be between 1 and " + totalCapacity + "."); return;
        }
        if (waitingQueue.contains(name) || customers.containsKey(name)) {
            handleError(ctx, 400, name + " is already in the system." ); return;
        }

        if(!insertCustomerIntoQueue(ctx, name, after, quantity)) return;

        updateCustomers();
        ctx.json(Map.of("status", "inserted"));
    }

    private boolean insertCustomerIntoQueue(Context ctx, String name, String after, int quantity) {
        try {
            WaiterInfo info = new WaiterInfo(quantity, System.currentTimeMillis());

            if (after.isEmpty()) {
                // blank "after" means insert at the front of the queue
                waitingQueue.enqueue(name, new WaiterInfo(quantity, System.currentTimeMillis()));
                return true;
            } else {
                if (!waitingQueue.contains(after)) {
                    handleError(ctx, 400, after + " is not in the waiting queue.");
                    return false;
                }
                waitingQueue.insertAfter(after, name, info);
                return true;
            }
        } catch (IllegalArgumentException e) {
            handleError(ctx, 400, e.getMessage()); return false;
        }
    }

    private void handleError(Context ctx, int status, String message) {
        ctx.status(status).json(Map.of("error", message));
    }

    private void removeExpired() {
        List<String> expired = new ArrayList<>();
        for (String name : customers.keySet()) {
            if (customers.get(name).expired()) expired.add(name);
        }
        for (String name : expired) customers.remove(name);
    }

    private void admitInWaiting(){
        // admit waiters from the front of the queue as long as there is capacity
        while (!waitingQueue.isEmpty()) {
            GenericQueue.Entry<WaiterInfo> front = waitingQueue.peek();
            if (usedCapacity() + front.value().quantity() <= totalCapacity) {
                waitingQueue.dequeue();
                customers.put(front.name(), new Customer(front.name(), front.value().quantity()));
            } else { break; }
        }
    }

    private void sendWaitingStatus(Context ctx, String name, int quantity, long waitSeconds) {
        ctx.json(Map.of(
                "status", "waiting",
                "position", waitingQueue.indexOfName(name) + 1,
                "waitSeconds", waitSeconds,
                "quantity", quantity ));
    }

    private void sendShoppingStatus(Context ctx, Customer customer) {
        ctx.json(Map.of(
                "status",      "shopping",
                "quantity",    customer.quantity,
                "secondsLeft", customer.secondsLeft() ));
    }

    public void savePurchase(String name, int quantity) {
        purchases.computeIfAbsent(name, k -> new ArrayList<>()).add(formatPurchase(quantity));
    }

    private String formatPurchase(int quantity){
        String timestamp = DateTimeFormatter
                .ofPattern("M/d/yyyy, h:mm:ss a")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());

        String record = quantity == 1 ? "ticket" : "tickets";
        return timestamp = ": " + quantity + record;
    }

    private int usedCapacity() {
        return customers.values().stream().mapToInt(s -> s.quantity).sum();
    }

}