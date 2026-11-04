package org.example.generator;

import org.example.classes.*;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest {

    private final Generator generator = new Generator();

    enum Color { RED, GREEN, BLUE }

    enum EmptyEnum {}

    static class Point {
        final int x;
        final int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    static class Person {
        final String name;
        final int age;
        final Color favoriteColor;
        final List<Point> route;

        Person(String name, int age, Color favoriteColor, List<Point> route) {
            this.name = name;
            this.age = age;
            this.favoriteColor = favoriteColor;
            this.route = route;
        }
    }

    @Generatable(implementations = {Dog.class, Cat.class})
    static abstract class Animal {
        abstract String sound();
    }

    static class Dog extends Animal {
        final String name;

        Dog(String name) {
            this.name = name;
        }

        String sound() {
            return "woof";
        }
    }

    static class Cat extends Animal {
        final int lives;

        Cat(int lives) {
            this.lives = lives;
        }

        String sound() {
            return "meow";
        }
    }

    interface Vehicle {
    }

    @Generatable
    interface EmptyAnnotated {
    }

    @Generatable(implementations = {String.class})
    interface WrongImplementation {
    }

    static class Broken {
        Broken() {
            throw new IllegalStateException("сломан");
        }
    }

    @Test
    void generatesPrimitives() {
        assertInstanceOf(Integer.class, generator.generateValueOfType(int.class));
        assertInstanceOf(Long.class, generator.generateValueOfType(long.class));
        assertInstanceOf(Double.class, generator.generateValueOfType(double.class));
        assertInstanceOf(Float.class, generator.generateValueOfType(float.class));
        assertInstanceOf(Short.class, generator.generateValueOfType(short.class));
        assertInstanceOf(Byte.class, generator.generateValueOfType(byte.class));
        assertInstanceOf(Boolean.class, generator.generateValueOfType(boolean.class));
        assertInstanceOf(Character.class, generator.generateValueOfType(char.class));
    }

    @Test
    void generatesWrappers() {
        Integer i = generator.generateValueOfType(Integer.class);
        Double d = generator.generateValueOfType(Double.class);
        Boolean b = generator.generateValueOfType(Boolean.class);
        Character c = generator.generateValueOfType(Character.class);

        assertNotNull(i);
        assertNotNull(d);
        assertNotNull(b);
        assertTrue(Character.isLetter(c));
    }

    @Test
    void generatesString() {
        String s = generator.generateValueOfType(String.class);
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    @Test
    void valuesAreNotConstant() {
        long distinct = java.util.stream.IntStream.range(0, 50)
                .mapToObj(i -> generator.generateValueOfType(String.class))
                .distinct()
                .count();
        assertTrue(distinct > 1);
    }

    @Test
    void generatesEnum() {
        Color color = generator.generateValueOfType(Color.class);
        assertNotNull(color);
        assertTrue(List.of(Color.values()).contains(color));
    }

    @Test
    void failsOnEmptyEnum() {
        assertThrows(GenerationException.class, () -> generator.generateValueOfType(EmptyEnum.class));
    }

    @Test
    void generatesPrimitiveArray() {
        int[] numbers = generator.generateValueOfType(int[].class);
        assertNotNull(numbers);
        assertTrue(numbers.length > 0);
    }

    @Test
    void generatesStringArray() {
        String[] strings = generator.generateValueOfType(String[].class);
        assertTrue(strings.length > 0);
        for (String s : strings) {
            assertNotNull(s);
        }
    }

    @Test
    void generatesEnumArray() {
        Color[] colors = generator.generateValueOfType(Color[].class);
        assertTrue(colors.length > 0);
        for (Color c : colors) {
            assertNotNull(c);
        }
    }

    @Test
    void generatesObjectArray() {
        Rectangle[] rectangles = generator.generateValueOfType(Rectangle[].class);
        assertTrue(rectangles.length > 0);
        for (Rectangle r : rectangles) {
            assertNotNull(r);
        }
    }

    @Test
    void generatesNonEmptyList() {
        List<?> list = generator.generateValueOfType(List.class);
        assertNotNull(list);
        assertFalse(list.isEmpty());
        for (Object item : list) {
            assertNotNull(item);
        }
    }

    @Test
    void generatesNonEmptyMap() {
        Map<?, ?> map = generator.generateValueOfType(Map.class);
        assertNotNull(map);
        assertFalse(map.isEmpty());
        map.forEach((k, v) -> {
            assertNotNull(k);
            assertNotNull(v);
        });
    }

    @Test
    void generatesExample() {
        assertNotNull(generator.generateValueOfType(Example.class));
    }

    @Test
    void generatesRectangle() {
        Rectangle r = generator.generateValueOfType(Rectangle.class);
        assertNotNull(r);
        assertTrue(r.getArea() >= 0);
        assertTrue(r.getPerimeter() >= 0);
    }

    @Test
    void generatesTriangle() {
        Triangle t = generator.generateValueOfType(Triangle.class);
        assertNotNull(t);
        assertTrue(t.getPerimeter() >= 0);
    }

    @Test
    void generatesProduct() {
        Product p = generator.generateValueOfType(Product.class);
        assertNotNull(p);
        assertNotNull(p.getName());
    }

    @Test
    void cartContainsProducts() {
        Cart cart = generator.generateValueOfType(Cart.class);
        assertNotNull(cart.getItems());
        assertFalse(cart.getItems().isEmpty());
        for (Object item : cart.getItems()) {
            assertInstanceOf(Product.class, item);
        }
    }

    @RepeatedTest(10)
    void binaryTreeHasLimitedDepth() {
        BinaryTreeNode root = generator.generateValueOfType(BinaryTreeNode.class);
        assertNotNull(root);
        assertNotNull(root.getData());
        assertTrue(height(root) <= 5);
    }

    private int height(BinaryTreeNode node) {
        if (node == null) {
            return 0;
        }
        return 1 + Math.max(height(node.getLeft()), height(node.getRight()));
    }

    @RepeatedTest(10)
    void generatesShapeImplementation() {
        Shape shape = generator.generateValueOfType(Shape.class);
        assertNotNull(shape);
        assertTrue(shape instanceof Rectangle || shape instanceof Triangle);
    }

    @Test
    void shapeUsesAllImplementations() {
        boolean sawRectangle = false;
        boolean sawTriangle = false;
        for (int i = 0; i < 100; i++) {
            Shape shape = generator.generateValueOfType(Shape.class);
            if (shape instanceof Rectangle) sawRectangle = true;
            if (shape instanceof Triangle) sawTriangle = true;
        }
        assertTrue(sawRectangle);
        assertTrue(sawTriangle);
    }

    @RepeatedTest(10)
    void generatesAbstractClassImplementation() {
        Animal animal = generator.generateValueOfType(Animal.class);
        assertNotNull(animal);
        assertTrue(animal instanceof Dog || animal instanceof Cat);
    }

    @Test
    void generatesCustomClass() {
        Point p = generator.generateValueOfType(Point.class);
        assertNotNull(p);
    }

    @Test
    void generatesNestedCustomClass() {
        Person person = generator.generateValueOfType(Person.class);
        assertNotNull(person.name);
        assertNotNull(person.favoriteColor);
        assertFalse(person.route.isEmpty());
        for (Object point : person.route) {
            assertInstanceOf(Point.class, point);
        }
    }

    @Test
    void failsOnInterfaceWithoutAnnotation() {
        GenerationException e = assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(Vehicle.class));
        assertTrue(e.getMessage().contains("Vehicle"));
    }

    @Test
    void failsOnAnnotationWithoutImplementations() {
        assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(EmptyAnnotated.class));
    }

    @Test
    void failsOnWrongImplementation() {
        assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(WrongImplementation.class));
    }

    @Test
    void failsWhenConstructorThrows() {
        GenerationException e = assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(Broken.class));
        assertNotNull(e.getCause());
    }
}