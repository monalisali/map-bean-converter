package com.github.hcsp.reflection;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MapBeanConverter {
    // 传入一个遵守Java Bean约定的对象，读取它的所有属性，存储成为一个Map
    // 例如，对于一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 应当返回一个Map { id -> 1, name -> "ABC", longName -> false }
    // 提示：
    //  1. 读取传入参数bean的Class
    //  2. 通过反射获得它包含的所有名为getXXX/isXXX，且无参数的方法（即getter方法）
    //  3. 通过反射调用这些方法并将获得的值存储到Map中返回
    public static Map<String, Object> beanToMap(Object bean) {
        List<String> methodNames = new ArrayList<>();
        HashMap<String, Object> result = new HashMap<>();
        Method[] methods = bean.getClass().getMethods();
        for (Method m : methods) {
            if (m.getName().startsWith("get") || m.getName().startsWith("is")) {
                methodNames.add(m.getName());
            }
        }

        for (String m : methodNames) {
            try {
                String key = m.startsWith("get") ? m.substring(3).toLowerCase() : StringUtils.uncapitalize(m.substring(2));
                result.put(key, bean.getClass().getMethod(m).invoke(bean));
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
        }

        return result;
    }

    // 传入一个遵守Java Bean约定的Class和一个Map，生成一个该对象的实例
    // 传入参数DemoJavaBean.class和Map { id -> 1, name -> "ABC"}
    // 应当返回一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 提示：
    //  1. 遍历map中的所有键值对，寻找klass中名为setXXX，且参数为对应值类型的方法（即setter方法）
    //  2. 使用反射创建klass对象的一个实例
    //  3. 使用反射调用setter方法对该实例的字段进行设值
    public static <T> T mapToBean(Class<T> klass, Map<String, Object> map) {
        List<String> methodNames;
        T obj = null;
        try {
            obj = klass.newInstance();
//            Method[] kMethods = klass.getMethods();
//            for (Method m : kMethods) {
//                if (m.getName().startsWith("set")) {
//                    methodNames.add(m.getName());
//                }
//            }

            methodNames = Stream.of(klass.getMethods())
                    .filter(m -> m.getName()
                            .startsWith("set"))
                    .map(Method::getName)
                    .collect(Collectors.toList());

            for (String m : methodNames) {
                String field = m.substring(3);
                String fieldName = StringUtils.uncapitalize(field);
                if (map.keySet().stream().anyMatch(x -> x.toLowerCase().equals(fieldName))) {
                    obj.getClass().getMethod(m, map.get(fieldName).getClass()).invoke(obj, map.get(fieldName));
                }
            }


        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }

        return obj;
    }

    public static void main(String[] args) {
        DemoJavaBean bean = new DemoJavaBean();
        bean.setId(100);
        bean.setName("AAAAAAAAAAAAAAAAAAA");
        System.out.println(beanToMap(bean));

        Map<String, Object> map = new HashMap<>();
        map.put("id", 123);
        map.put("name", "ABCDEFG");
        System.out.println(mapToBean(DemoJavaBean.class, map));
    }

    static class DemoJavaBean {
        Integer id;
        String name;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isLongName() {
            return name.length() > 10;
        }

        @Override
        public String toString() {
            return "DemoJavaBean{"
                    + "id="
                    + id
                    + ", name='"
                    + name
                    + '\''
                    + ", longName="
                    + isLongName()
                    + '}';
        }
    }
}
