package ru.ifmo.rain.kononov.student;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedStudentGroupQuery {
    private static final Comparator<Student> NAME_COMPARATOR = Comparator
            .comparing(Student::getLastName, String::compareTo)
            .thenComparing(Student::getFirstName, String::compareTo)
            .thenComparing(Student::compareTo);

    private <T extends Collection<String>> T transform(List<Student> students, Function<Student, String> functor, Collector<String, ?, T> collector) {
        return students
                .stream()
                .map(functor)
                .collect(collector);
    }

    private List<Student> sort(Collection<Student> students, Comparator<Student> comparator) {
        return students
                .stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private <T> T filterAndSort(Collection<Student> students, Predicate<Student> predicate, Collector<Student, ?, T> collector) {
        return students
                .stream()
                .filter(predicate)
                .sorted(NAME_COMPARATOR)
                .collect(collector);
    }

    private Stream<Map.Entry<String, List<Student>>> grouping(Collection<Student> students) {
        return students
                .stream()
                .collect(Collectors.groupingBy(Student::getGroup))
                .entrySet()
                .stream();
    }

    private List<Group> sortedGroups(Collection<Student> students, Function<List<Student>, List<Student>> functor) {
        return grouping(students)
                .map(entry -> new Group(entry.getKey(), functor.apply(entry.getValue())))
                .sorted(Comparator.comparing(Group::getName))
                .collect(Collectors.toList());
    }

    private String largestGroupName(Collection<Student> students, Function<List<Student>, Integer> functor) {
        return grouping(students)
                .max(Comparator
                        .comparingInt((Map.Entry<String, List<Student>> entry) -> functor.apply(entry.getValue()))
                        .thenComparing(Map.Entry::getKey, Collections.reverseOrder(String::compareTo)))
                .map(Map.Entry::getKey).orElse("");
    }

    private String fullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    private Predicate<Student> getStudentPredicate(Function<Student, String> method, String name) {
        return student -> method.apply(student).equals(name);
    }

    public List<String> getFirstNames(List<Student> students) {
        return transform(students, Student::getFirstName, Collectors.toList());
    }

    public List<String> getLastNames(List<Student> students) {
        return transform(students, Student::getLastName, Collectors.toList());
    }

    public List<String> getGroups(List<Student> students) {
        return transform(students, Student::getGroup, Collectors.toList());
    }

    public List<String> getFullNames(List<Student> students) {
        return transform(students, this::fullName, Collectors.toList());
    }

    public Set<String> getDistinctFirstNames(List<Student> students) {
        return transform(students, Student::getFirstName, Collectors.toCollection(TreeSet::new));
    }

    public String getMinStudentFirstName(List<Student> students) {
        return students
                .stream()
                .min(Student::compareTo)
                .map(Student::getFirstName)
                .orElse("");
    }

    public List<Student> sortStudentsById(Collection<Student> students) {
        return sort(students, Student::compareTo);
    }

    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sort(students, NAME_COMPARATOR);
    }

    private List<Student> findStudentsBy(Collection<Student> students, String str, Function<Student, String> method){
        return filterAndSort(students,
                getStudentPredicate(method, str),
                Collectors.toList());
    }

    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsBy(students, name, Student::getFirstName);
    }

    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsBy(students, name, Student::getLastName);
    }

    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return findStudentsBy(students, group, Student::getGroup);
    }

    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return filterAndSort(students,
                getStudentPredicate(Student::getGroup, group),
                Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }

    public List<Group> getGroupsByName(Collection<Student> students) {
        return sortedGroups(students, this::sortStudentsByName);
    }

    public List<Group> getGroupsById(Collection<Student> students) {
        return sortedGroups(students, this::sortStudentsById);
    }

    public String getLargestGroup(Collection<Student> students) {
        return largestGroupName(students, List::size);
    }

    public String getLargestGroupFirstName(Collection<Student> students) {
        return largestGroupName(students, list -> getDistinctFirstNames(list).size());
    }

    public String getMostPopularName(Collection<Student> collection) {
        return collection.stream()
                .collect(Collectors.groupingBy(this::fullName, Collectors.mapping(Student::getGroup, Collectors.collectingAndThen(Collectors.toSet(), Set::size))))
                .entrySet()
                .stream()
                .max(Comparator
                        .comparing((Function<Map.Entry<String, Integer>, Integer>) Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .orElse("");
    }
}