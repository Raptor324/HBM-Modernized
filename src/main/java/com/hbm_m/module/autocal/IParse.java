package com.hbm_m.module.autocal;

import java.util.HashMap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.module.IParse}: Interpreter-Schnittstelle des AUTOCAL samt Laufzeitkontext. */
public interface IParse {

    EnumStatementReturn eval(ParseContext ctx, String line);
    void generateJumpPoints(ParseContext ctx, String line, int index);

    class ParseContext {
        public Level world;
        public CompoundTag variables = new CompoundTag();
        public HashMap<String, Integer> jmp = new HashMap<>();

        public static final int MAX_BUFFER_LENGTH = 256;
        public static final int MAX_STACK_SIZE = 256;

        private String buffer = "";
        private String[] stack = new String[MAX_STACK_SIZE];
        private int stackSize = 0;
        public String splitString = ";";
        public int clockSpeed = 1;
        public int current = 0;

        public ParseContext(Level world) {
            this.world = world;

            for (int i = 0; i < MAX_STACK_SIZE; i++) stack[i] = "";
        }

        public String readBuffer() {
            return this.buffer;
        }

        /** Sets the buffer and imposes length restrictions. Returns true if successful, and false if truncation has taken place. */
        public boolean writeBuffer(String buffer) {

            if (buffer.length() > MAX_BUFFER_LENGTH) {
                this.buffer = buffer.substring(0, MAX_BUFFER_LENGTH);
                return false;
            }

            this.buffer = buffer;
            return true;
        }

        public boolean push(String line) {
            if (stackSize >= MAX_STACK_SIZE) return false;
            if (line.length() > MAX_BUFFER_LENGTH) line = line.substring(0, MAX_BUFFER_LENGTH);
            stack[stackSize] = line;
            stackSize++;
            return true;
        }

        public String pop() {
            if (stackSize <= 0) return null;
            if (stackSize > MAX_STACK_SIZE) stackSize = MAX_STACK_SIZE;
            String ret = stack[stackSize - 1];
            stack[stackSize - 1] = "";
            stackSize--;
            return ret;
        }

        public String peek() {
            if (stackSize <= 0) return null;
            if (stackSize > MAX_STACK_SIZE) stackSize = MAX_STACK_SIZE;
            return stack[stackSize - 1];
        }

        public void turnOff() {
            this.clockSpeed = 1;
            this.current = 0;
            this.buffer = "";
            if (!this.variables.isEmpty()) this.variables = new CompoundTag();
        }

        // NBT R/W is now in control of the CTX, meaning additions to the AUTOCAL runtime should be a lot easier
        public void readFromNBT(CompoundTag nbt, String[] script, IParse parser) {
            current = nbt.getInt("current");
            clockSpeed = nbt.getInt("clockSpeed");
            buffer = nbt.getString("buffer");
            splitString = nbt.getString("splitString");
            variables = nbt.getCompound("variables");

            stackSize = nbt.getInt("stackSize");
            for (int i = 0; i < MAX_STACK_SIZE; i++) stack[i] = nbt.getString("st" + i);
            for (int i = 0; i < script.length; i++) parser.generateJumpPoints(this, script[i], i);
        }

        public void writeToNBT(CompoundTag nbt) {
            nbt.putInt("current", current);
            nbt.putInt("clockSpeed", clockSpeed);
            nbt.putString("buffer", buffer);
            nbt.putString("splitString", splitString);
            nbt.put("variables", variables);

            nbt.putInt("stackSize", stackSize);
            for (int i = 0; i < MAX_STACK_SIZE; i++) nbt.putString("st" + i, stack[i]);
        }
    }

    enum EnumStatementReturn {
        /** The command executed correctly (more or less) */
        OK,
        /** The command hasn't been recognized */
        UNRECOGNIZED_COMMAND,
        /** The expected parameters aren't present, or the parameters couldn't be parsed (i.e. using an undefined jump point) */
        PARAMETER_ERROR,
        /** Requests the AUTOCAL unit to end the tick, regardless of how many clock cycles are left */
        END_TICK,
        /** Requests an AUTOCAL shutdown */
        SHUTDOWN,
        /** Skips the instruction, doesn't use up a clock cycle */
        SKIP,
        /** General undefined behavior */
        UNDEFINED,
        /** Stack ran full */
        STACK_EXCEEDED
    }
}
