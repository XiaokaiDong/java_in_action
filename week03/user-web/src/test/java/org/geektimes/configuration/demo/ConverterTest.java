package org.geektimes.configuration.demo;


import org.geektimes.config.converter.MyAbstractTypeConverter;
import org.geektimes.config.converter.impl.ShortConverter;

public class ConverterTest {
    public static void main(String[] args) {

        ShortConverter shortConverter = new ShortConverter();
        System.out.println(shortConverter.resolveConvertedType(shortConverter));
        int result = shortConverter.convert("36");
        System.out.println(result);

        MyAbstractTypeConverter<Boolean> booleanConverter = new MyAbstractTypeConverter<Boolean>() {
            @Override
            protected Boolean doConvert(String value) {
                return null;
            }
        };
        System.out.println(booleanConverter.resolveConvertedType(booleanConverter));


    }
}
