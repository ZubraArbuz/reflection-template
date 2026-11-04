package org.example.generator;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;

public class Generator {
    private static final int MAX_DEPTH = 3;
    private static final int MAX_SIZE = 5;
    private final Random random = new Random();
    private final Map<Class<?>, Supplier<Object>> simpleTypes = new HashMap<>();

    public Generator(){
        register(int.class, Integer.class, () -> random.nextInt(1000));
        register(long.class, Long.class, () -> random.nextLong(1000));
        register(short.class, Short.class, () -> (short) random.nextInt(100));
        register(byte.class, Byte.class, () -> (byte) random.nextInt(100));
        register(float.class, Float.class, () -> random.nextFloat() * 1000);
        register(double.class, Double.class, () -> random.nextDouble() * 1000);
        register(boolean.class, Boolean.class, () -> random.nextBoolean());
        register(char.class, Character.class, () -> (char) ('a' + random.nextInt(26)));
        simpleTypes.put(String.class, this::randomString);
    }

    private void register(Class<?> primitive, Class<?> wrapper, Supplier<Object> supplier){
        simpleTypes.put(primitive, supplier);
        simpleTypes.put(wrapper, supplier);
    }

    @SuppressWarnings("unchecked")
    public <T> T generateValueOfType(Class<T> clazz){
        return (T) generate(clazz, 0);
    }
    private Object generate(Type type, int depth){
        if (type instanceof Class<?> clazz) {
            return generateClass(clazz, new Type[0], depth);
        }
        if (type instanceof ParameterizedType parameterized) {
            Class<?> raw = (Class<?>) parameterized.getRawType();
            return generateClass(raw, parameterized.getActualTypeArguments(), depth);
        }
        throw new GenerationException("Неподдерживаемый тип данных " + type);
    }

    private Object generateClass(Class<?> clazz, Type[] typeArgs, int depth){
        if(simpleTypes.containsKey(clazz)){
            return simpleTypes.get(clazz).get();
        }
        if(clazz.isEnum()){
            Object[] constants = clazz.getEnumConstants();
            if(constants.length == 0){
                throw new GenerationException("В перечислении " + clazz.getName() + " нет значений");
            }
            return constants[random.nextInt(constants.length)];
        }
        if (clazz.isArray()){
            return generateArray(clazz.getComponentType(), depth);
        }
        if (clazz == List.class){
            Type elementType = typeArgs.length > 0 ? typeArgs[0] : Integer.class;
            List<Object> list = new ArrayList<>();
            int size = 1 + random.nextInt(MAX_SIZE);
            for (int i = 0; i < size; i++){
                list.add(generate(elementType, depth + 1));
            }
            return list;
        }
        if (clazz == Map.class){
            Type keyType = typeArgs.length > 0 ? typeArgs[0] : String.class;
            Type valueType = typeArgs.length > 1 ? typeArgs[1] : Integer.class;
            Map<Object, Object> map = new HashMap<>();
            int size = 1 + random.nextInt(MAX_SIZE);
            for(int i = 0; i < size; i++){
                map.put(generate(keyType, depth + 1), generate(valueType, depth + 1));
            }
            return map;
        }
        if(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            return generateImplementation(clazz, depth);
        }
        return generateObject(clazz, depth);
    }
    private Object generateArray(Class<?> componentType, int depth){
        int length = 1 + random.nextInt(MAX_SIZE);
        Object array = Array.newInstance(componentType, length);
        for(int i = 0; i < length; i++){
            Array.set(array, i, generate(componentType, depth + 1));
        }
        return array;
    }
    private Object generateImplementation(Class<?> clazz, int depth){
        Generatable annotation = clazz.getAnnotation(Generatable.class);
        if(annotation == null || annotation.implementations().length == 0){
            throw new GenerationException("Нельзя создать " + clazz.getName() + " это интерфейс, укажите реализации в аннотации @Generatable(implementations = {})");
        }
        Class<?>[] implementations = annotation.implementations();
        Class<?> chosen = implementations[random.nextInt(implementations.length)];
        if(!clazz.isAssignableFrom(chosen)){
            throw new GenerationException(chosen.getName() + " не является реализацией " + clazz.getName());
        }
        return generateClass(chosen, new Type[0], depth);
    }

    private Object generateObject(Class<?> clazz, int depth){
        List<Constructor<?>> constructors = new ArrayList<>(Arrays.asList(clazz.getDeclaredConstructors()));
        if(constructors.isEmpty()){
            throw new GenerationException(clazz.getName() + " не имеет конструкторов");
        }
        Collections.shuffle(constructors,random);
        Exception lastError = null;
        for(Constructor<?> constructor : constructors){
            try{
                Type[] paramTypes = constructor.getGenericParameterTypes();
                Object[] args = new Object[paramTypes.length];
                for(int i = 0; i < paramTypes.length; i++){
                    args[i] = generateArgument(paramTypes[i], depth + 1);
                }
                constructor.setAccessible(true);
                return constructor.newInstance(args);
            } catch (Exception e){
                lastError = e;
            }
        }
        throw new GenerationException("не удалось создать объект " + clazz.getName(),lastError);
    }
    private Object generateArgument(Type type,int depth){
        Class<?> raw = type instanceof ParameterizedType p ? (Class<?>) p.getRawType() : (Class<?>) type;
        boolean simple = raw.isPrimitive() || simpleTypes.containsKey(raw) || raw.isEnum();
        if(depth > MAX_DEPTH && !simple){
            return null;
        }
        return generate(type, depth);
    }

    private String randomString() {
        int length = 3 + random.nextInt(8);
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < length; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return sb.toString();
    }

}
