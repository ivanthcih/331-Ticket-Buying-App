package genericqueue;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class GenericQueueSpecTest {

    @Test
    public void entriesEmpty() {
        GenericQueue<String> q = new GenericQueueImpl<String>();
        assertEquals(List.of(), q.entries());
    }

    @Test
    public void testEmptyQueue() {
        GenericQueue<String> q = new GenericQueueImpl<>();
        assertEquals(0, q.size());
        assertTrue(q.isEmpty());
        assertNull(q.peek());
    }

    @Test
    public void testPeek() {
        GenericQueue<String> q = new GenericQueueImpl<>();
        GenericQueue<String> thrower = new GenericQueueImpl<>();
        GenericQueue.Entry<String> e = new GenericQueue.Entry<>("Alex", "Can I eat here?");
        q.enqueue("Alex", "Can I eat here?");
        assertEquals(e, q.peek());
        assertNull(thrower.peek());
    }

    @Test
    public void testContains() {
        GenericQueue<String> q = new GenericQueueImpl<>();
        q.enqueue("Alex", "Can I eat here?");
        assertTrue(q.contains("Alex"));
        assertFalse(q.contains("James"));
    }

    @Test
    public void testRemove() {
        GenericQueue<String> q = new GenericQueueImpl<>();
        GenericQueue.Entry<String> e = new GenericQueue.Entry<>("James",
                "Where can I expect to find my midterm results?");
        q.enqueue("Alex", "Can I eat here?");
        q.enqueue("James", "Where can I expect to find my midterm results?");
        q.enqueue("Alyssa", "How do I write within while loops?");
        q.remove("Alex");
        assertEquals(2, q.size());
        assertEquals(e, q.peek());
    }

    @Test
    public void testEnqueue() {
        GenericQueue<String> q = new GenericQueueImpl<>();
        GenericQueue.Entry<String> e = new GenericQueue.Entry<>("Alex", "Can I eat here?");

        assertThrows(IllegalArgumentException.class, () -> q.enqueue(null, "Can I eat here?"));
        assertThrows(IllegalArgumentException.class, () -> q.enqueue("", "Can I eat here?"));
        q.enqueue(e.name(), e.value());
        assertThrows(IllegalArgumentException.class, () -> q.enqueue("Alex", "Where can I expect to find my midterm results?"));
        assertEquals(1, q.size());
        assertEquals(e, q.peek());

        q.enqueue("James", "What is polymorphism?");
        q.enqueue("Alyssa", "How do I write within while loops?");
        assertEquals(3, q.size());
        assertEquals(e, q.peek());
    }

    @Test
    public void testDequeue() {
        GenericQueue<String> q = new GenericQueueImpl<>();

        assertThrows(IllegalStateException.class, () -> q.dequeue());

        q.enqueue("Alex", "Can I eat here?");
        q.enqueue("James", "Where can I expect to find my midterm results?");
        q.enqueue("Alyssa", "How do I write within while loops?");
        assertEquals(3, q.size());
        assertEquals(new GenericQueue.Entry<>("Alex", "Can I eat here?"), q.dequeue());
        assertEquals(new GenericQueue.Entry<>("James", "Where can I expect to find my midterm results?"), q.dequeue());
        assertEquals(new GenericQueue.Entry<>("Alyssa", "How do I write within while loops?"), q.dequeue());
        assertEquals(0, q.size());
    }

    @Test
    public void testInsertAfter() {
        GenericQueue<String> thrower = new GenericQueueImpl<>();
        GenericQueue<String> q = new GenericQueueImpl<>();
        GenericQueue<String> h = new GenericQueueImpl<>();

        thrower.enqueue("Alex", "Can I eat here?");
        assertThrows(IllegalArgumentException.class, () -> thrower.insertAfter("Alex", null, "Can I eat here?"));
        assertThrows(IllegalArgumentException.class, () -> thrower.insertAfter(null, "Alex", "Can I eat here?"));

        q.enqueue("Alex", "Can I eat here?");
        q.enqueue("James", "Where can I expect to find my midterm results?");
        q.insertAfter("Alex", "Alyssa", "How do I write within while loops?");
        assertEquals(3, q.size());
        q.dequeue();
        assertEquals(new GenericQueue.Entry<>("Alyssa", "How do I write within while loops?"), q.peek());

        h.enqueue("Alex", "Can I eat here?");
        h.enqueue("James", "Where can I expect to find my midterm results?");
        h.insertAfter("Alex", "Alyssa", "How do I write within while loops?");
        h.insertAfter("Alex", "Kyle", "Can I get a regrade for HW 3?");
        assertEquals(4, h.size());
        h.dequeue();
        assertEquals(new GenericQueue.Entry<>("Kyle", "Can I get a regrade for HW 3?"), h.peek());
        h.dequeue();
        assertEquals(new GenericQueue.Entry<>("Alyssa", "How do I write within while loops?"), h.peek());
    }

    @Test
    public void testIndexOfName() {
        GenericQueue<String> q = new GenericQueueImpl<>();
        q.enqueue("Alex", "Can I eat here?");
        q.enqueue("James", "Where can I expect to find my midterm results?");
        assertEquals(1, q.indexOfName("James"));
    }

    @Test
    public void testEntries() {
        GenericQueue<String> q = new GenericQueueImpl<>();
        q.enqueue("Alex", "Can I eat here?");
        q.enqueue("James", "Where can I expect to find my midterm results?");
        q.enqueue("Alyssa", "How do I write within while loops?");
        List<GenericQueue.Entry<String>> entries = q.entries();
        assertEquals(3, entries.size());
        assertEquals(new GenericQueue.Entry<>("Alex", "Can I eat here?"), entries.get(0));
        assertEquals(new GenericQueue.Entry<>("James", "Where can I expect to find my midterm results?"), entries.get(1));
        assertEquals(new GenericQueue.Entry<>("Alyssa", "How do I write within while loops?"), entries.get(2));
    }

    @Test
    public void testGetEntry() {
        GenericQueue<String> q = new GenericQueueImpl<>();
        q.enqueue("Alex", "Can I eat here?");
        q.enqueue("James", "Where can I expect to find my midterm results?");
        q.enqueue("Alyssa", "How do I write within while loops?");
        assertEquals(new GenericQueue.Entry<>("Alex", "Can I eat here?"), q.getEntry("Alex"));
        assertEquals(new GenericQueue.Entry<>("James", "Where can I expect to find my midterm results?"), q.getEntry("James"));
        assertEquals(new GenericQueue.Entry<>("Alyssa", "How do I write within while loops?"), q.getEntry("Alyssa"));
    }

    @Test
    public void testUpdateValue() {
        GenericQueue<String> q = new GenericQueueImpl<>();
        q.enqueue("Alex", "Can I eat here?");
        q.enqueue("James", "Where can I expect to find my midterm results?");
        q.enqueue("Alyssa", "How do I write within while loops?");
        q.updateValue("Alex", "Can I eat here? Yes, I can.");
        assertEquals(new GenericQueue.Entry<>("Alex", "Can I eat here? Yes, I can."), q.getEntry("Alex"));
    }

    @Test
    public void testRename() {
        GenericQueue<String> q = new GenericQueueImpl<>();
        q.enqueue("Alex", "Can I eat here?");
        q.enqueue("James", "Where can I expect to find my midterm results?");
        q.enqueue("Alyssa", "How do I write within while loops?");
        q.rename("Alyssa", "Alexandra");
        assertEquals(3, q.size());
        assertFalse(q.contains("Alyssa"));
        assertTrue(q.contains("Alexandra"));
        assertEquals(new GenericQueue.Entry<>("Alexandra", "How do I write within while loops?"), q.getEntry("Alexandra"));
    }
}