package genericqueue;

import java.util.*;

public class GenericQueueImpl<V> implements GenericQueue<V> {

    List<String> studentNames;
    List<V> studentQuestions;
    HashMap<String, V> studentQuestionsMap; // Names and Questions
    HashMap<String, Integer> nameIndexMap;
    private static final int FIRST_INDEX = 0;

    /**
     * RI:
     *  studentNames.size()
     *      == studentQuestions.size()
     *      == studentQuestionsMap.size()
     *      == nameToIndex.size()
     *  studentNames and studentQuestions are both ordered by most recent
     *  studentNames contains no null, empty, nor duplicate Strings
     *  studentQuestions may contain null or empty Strings
     *  for every i in [0, studentNames.size()):
     *      name = studentNames.get(i)
     *      studentQuestionsMap.containsKey(studentNames.get(i))
     *      studentQuestionsMap.get(studentNames.get(i)).equals(studentQuestions.get(studentNames.get(i)))
     *      nameIndexMap.containsKey(studentNames.get(i))
     *      nameIndexMap.get(studentNames.get(i)) == i
     *  FIRST_INDEX == 0
     * AF:
     *  AF(this) = an entry of (student_name, question) pairs in arrival order where:
     *  the i-th student in line is studentNames.get(i) with question studentQuestions.get(i)
     *  and the same paring is accessible by name via studentQuestionsMap.
     *  The front of the queue is the earliest student_name added and is at
     *  studentNames.get(FIRST_INDEX) with question studentQuestions.get(FIRST_INDEX).
     *  nameIndexMap maps each name to its current 0-based position in studentName,
     *  providing O(1) positional lookup without scanning the list.
     */

    /**
     * Creates an empty office hours queue.
     */
    public GenericQueueImpl() {
        studentNames = new ArrayList<>();
        studentQuestions = new ArrayList<>();
        studentQuestionsMap = new HashMap<>();
        nameIndexMap = new HashMap<>();
        checkRep();
    }

    @Override
    public int size() { checkRep(); return studentNames.size();
    }

    @Override
    public boolean isEmpty() { checkRep(); return studentNames.isEmpty(); }

    @Override
    public void enqueue(String name, V question) {
        checkRep();
        // checks: name is non-null/empty and not in queue
        if(name == null || name.isEmpty()) { throw new IllegalArgumentException("name doesn't exist!"); }
        else if(studentNames.contains(name)) { throw new IllegalArgumentException(name + " is already in queue"); }

        studentQuestionsMap.put(name, question);
        nameIndexMap.put(name, studentNames.size());
        studentNames.add(name);
        studentQuestions.add(question);
        checkRep();
    }

    @Override
    public Entry<V> dequeue() {
        checkRep();
        if(studentNames.isEmpty()) { throw new IllegalStateException("Queue is empty!"); }
        String name = studentNames.remove(FIRST_INDEX);
        V question = studentQuestions.remove(FIRST_INDEX);
        studentQuestionsMap.remove(name);
        nameIndexMap.remove(name);
        for(String n : nameIndexMap.keySet()) {
            nameIndexMap.put(n, nameIndexMap.get(n)-1);;
        }
        checkRep();
        return new Entry<>(name, question);
    }

    @Override
    public boolean remove(String name) {
        checkRep();
        if(studentNames.contains(name)) {
            int index = studentNames.indexOf(name);
            studentNames.remove(index);
            studentQuestions.remove(index);
            for(int i = index; i < studentNames.size(); i++) { nameIndexMap.put(studentNames.get(i), i); }
            studentQuestionsMap.remove(name);
            checkRep();
            return true;
        }
        checkRep();
        return false;
    }

    @Override
    public void insertAfter(String existingName, String name, V question) {
        checkRep();
        // checks: name is non-null or not in queue, existingName is in queue and non-null
        if(name == null || existingName == null) { throw new IllegalArgumentException("Name or existingName cannot be null");}
        else if(!studentNames.contains(existingName)) { throw new IllegalArgumentException("Existing name not in queue");}
        else if(studentNames.contains(name)) { throw new IllegalArgumentException("Student already in queue"); }

        int index = studentNames.indexOf(existingName) + 1;
        studentNames.add(index, name);
        studentQuestions.add(index, question);
        studentQuestionsMap.put(name, question);
        nameIndexMap.put(name, index);
        for(int i = index; i < studentNames.size(); i++) { nameIndexMap.put(studentNames.get(i), i); }
        checkRep();
    }

    @Override
    public Entry<V> peek() {
        checkRep();
        if(studentNames.isEmpty()) { return null; }
        return new Entry<>(studentNames.get(FIRST_INDEX), studentQuestions.get(FIRST_INDEX));
    }

    @Override
    public boolean contains(String name) { checkRep(); return studentQuestionsMap.containsKey(name); }

    @Override
    public int indexOfName(String name) {
        checkRep();
        Integer index = nameIndexMap.get(name);
        if(index != null){ return index; }
        return -1;
    }

    @Override
    public List<Entry<V>> entries() {
        checkRep();
        List<Entry<V>> entries = new ArrayList<>();

        for(int i = 0; i < studentNames.size(); i++) {
            Entry<V> entry = new Entry<V>(studentNames.get(i), studentQuestions.get(i));
            entries.add(entry);
        }

        return entries;
    }

    @Override
    public Entry<V> getEntry(String name) {
        checkRep();
        if (!studentQuestionsMap.containsKey(name)) { throw new IllegalArgumentException(name + " not in queue"); }
        return new Entry<>(name, studentQuestionsMap.get(name));
    }

    @Override
    public void updateValue(String name, V question) {
        checkRep();
        if(name == null){ throw new IllegalArgumentException("Names cannot be null"); }
        int index = nameIndexMap.get(name);
        studentQuestionsMap.put(name, question);
        studentQuestions.set(index, question);
        checkRep();
    }

    @Override
    public void rename(String oldName, String newName) {
        checkRep();
        if(oldName == null || newName == null) { throw new IllegalArgumentException("Names cannot be null"); }
        if(oldName.equals(newName)) { return;}
        int index = nameIndexMap.get(oldName);
        studentNames.set(index, newName);
        studentQuestionsMap.put(newName, studentQuestionsMap.get(oldName));
        studentQuestionsMap.remove(oldName);
        nameIndexMap.put(newName, index);
        nameIndexMap.remove(oldName);
        checkRep();
    }

    private void checkRep(){
        assert studentNames != null : "studentNames is null";
        assert studentQuestions != null : "studentQuestions is null";
        assert studentQuestionsMap != null : "studentQuestionsMap is null";

        assert studentNames.size() == studentQuestions.size()
                : "studentNames and studentQuestions are not the same size";
        assert studentNames.size() == studentQuestionsMap.size()
                : "studentNames and studentQuestionsMap are not the same size";

        Set<String> seen = new HashSet<>();
        for(int i = 0; i < studentNames.size(); i++) {
            String name = studentNames.get(i);
            V question = studentQuestions.get(i);

            assert name != null && !name.isEmpty() : "studentNames contains null or empty name at index " + i;
            assert !seen.contains(name) : "studentNames contains duplicate name at index " + i + ": " + name;
            seen.add(name);

            assert studentQuestionsMap.containsKey(name) : "map missing key: " + name;

            V mappedQ = studentQuestionsMap.get(name);
            assert mappedQ == null && question == null || (mappedQ != null && mappedQ.equals(question)) : "map value does not match question for name: " + name;
            assert nameIndexMap.containsKey(name) : "nameIndexMap missing key: " + name;
            assert nameIndexMap.get(name) == i : "nameIndexMap value does not match index for name: " + name;
        }

        assert FIRST_INDEX == 0 : "FIRST_INDEX is not 0";
    }

}
