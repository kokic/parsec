package foreign;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Objects;

public final class CLib {

    public static final Linker linker = Linker.nativeLinker();
    public static final SymbolLookup lookup = linker.defaultLookup();
    public static final SegmentAllocator allocator = SegmentAllocator.implicitAllocator();

    public static final ValueLayout.OfAddress Addr = ValueLayout.ADDRESS;
    public static final ValueLayout.OfByte Byte = ValueLayout.JAVA_BYTE;
    public static final ValueLayout.OfChar Char = ValueLayout.JAVA_CHAR;
    public static final ValueLayout.OfShort Short = ValueLayout.JAVA_SHORT;
    public static final ValueLayout.OfInt Int = ValueLayout.JAVA_INT;
    public static final ValueLayout.OfLong Long = ValueLayout.JAVA_LONG;
    public static final ValueLayout.OfFloat Float = ValueLayout.JAVA_FLOAT;
    public static final ValueLayout.OfDouble Double = ValueLayout.JAVA_DOUBLE;

    public static MemorySegment string(String str) {
        return allocator.allocateUtf8String(str);
    }

    public static MethodHandle downcall(String name, FunctionDescriptor descriptor) {
        return linker.downcallHandle(lookup.lookup(name).get(), descriptor);
    }

    public static MethodHandle downcall(String name, MemoryLayout... argLayouts) {
        return downcall(name, FunctionDescriptor.ofVoid(argLayouts));
    }

    public static MethodHandle downcall(MemoryLayout resLayout, String name,
            MemoryLayout... argLayouts) {
        if (Objects.isNull(resLayout))
            return downcall(name, argLayouts);
        return downcall(name, FunctionDescriptor.of(resLayout, argLayouts));
    }

    public static void main(String[] args) throws Throwable {

        var printf = new NFInfer("printf");
        printf.invoke("%s from %s, %d\n", "Hello World", "C", 2022);

        var exp = new NFInfer("exp", Double);
        var sqrt = new NFInfer("sqrt", Double);
        var value = (double) sqrt.invoke(163d);

        System.out.printf("%.16f", exp.invoke(Math.PI * value));
    }
}