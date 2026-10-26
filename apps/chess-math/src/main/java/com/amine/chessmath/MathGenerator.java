package com.amine.chessmath;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Balanced family rotation and shuffled candidate bags, reproducible after recovery. */
public final class MathGenerator {
    private static final String[] OPERATIONS = {Exercise.ADD, Exercise.SUBTRACT, Exercise.MULTIPLY};
    private long randomState;
    private long nextId = 1;
    private MathConfig config;
    private Exercise last;
    private Bag families;
    private final Bag[] candidates = new Bag[3];

    public MathGenerator(long seed) { randomState = seed; }

    public Exercise next(MathConfig settings) {
        if (settings == null) throw new IllegalArgumentException("Instellingen ontbreken.");
        settings.validate();
        if (nextId == Long.MAX_VALUE) throw new IllegalStateException("Geen nieuwe som beschikbaar.");
        if (config == null || !sameConfig(config, settings)) configure(settings);
        if (families.exhausted()) families = new Bag(families.source, nextSeed(), -1);
        int family = families.take();
        Bag bag = candidates[family];
        if (bag.exhausted()) {
            bag = new Bag(bag.source, nextSeed(), last != null && last.operation.equals(OPERATIONS[family])
                    ? last.a * 101 + last.b : -1);
            candidates[family] = bag;
        }
        int pair = bag.take();
        last = new Exercise(nextId++, pair / 101, pair % 101, OPERATIONS[family]);
        return last;
    }

    private void configure(MathConfig settings) {
        config = settings.copy();
        List<Integer> enabled = new ArrayList<>();
        for (int family = 0; family < 3; family++) {
            boolean use = family == 0 ? config.addition : family == 1 ? config.subtraction : config.multiplication;
            if (use) {
                enabled.add(family);
                candidates[family] = new Bag(pool(config, family), nextSeed(),
                        last != null && last.operation.equals(OPERATIONS[family]) ? last.a * 101 + last.b : -1);
            } else candidates[family] = null;
        }
        families = new Bag(enabled, nextSeed(), -1);
    }

    private static List<Integer> pool(MathConfig c, int family) {
        List<Integer> values = new ArrayList<>();
        if (family == 2) {
            for (int table = 1; table <= 10; table++) if (c.tables[table])
                for (int multiplier = 1; multiplier <= 10; multiplier++) values.add(multiplier * 101 + table);
        } else {
            for (int a = 0; a <= c.limit; a++) for (int b = 0; b <= c.limit; b++) {
                boolean allowed = family == 0 ? a + b <= c.limit && (c.crossingTen || a % 10 + b % 10 < 10)
                        : b <= a && (c.crossingTen || a % 10 >= b % 10);
                if (allowed) values.add(a * 101 + b);
            }
        }
        return values;
    }

    private long nextSeed() {
        // SplitMix64: persist the entire state rather than relying on inaccessible Random internals.
        long z = (randomState += 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public String snapshot() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(1); out.writeLong(randomState); out.writeLong(nextId);
            out.writeBoolean(config != null);
            if (config != null) {
                out.writeInt(config.limit); out.writeBoolean(config.addition); out.writeBoolean(config.subtraction);
                out.writeBoolean(config.multiplication); out.writeBoolean(config.crossingTen);
                for (int i = 1; i <= 10; i++) out.writeBoolean(config.tables[i]);
                writeBag(out, families);
                for (Bag candidate : candidates) writeBag(out, candidate);
            }
            out.writeBoolean(last != null);
            if (last != null) {
                out.writeLong(last.id); out.writeInt(last.a); out.writeInt(last.b); out.writeUTF(last.operation);
            }
            out.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public static MathGenerator restore(String snapshot) {
        if (snapshot == null || snapshot.length() > 1024) throw new IllegalArgumentException("Ongeldige somopslag.");
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(snapshot)));
            if (in.readInt() != 1) throw new IllegalArgumentException("Onbekende somopslag.");
            MathGenerator generator = new MathGenerator(in.readLong());
            generator.nextId = in.readLong();
            if (generator.nextId < 1) throw new IllegalArgumentException("Ongeldig somnummer.");
            if (in.readBoolean()) {
                MathConfig c = new MathConfig(); c.limit = in.readInt(); c.addition = in.readBoolean();
                c.subtraction = in.readBoolean(); c.multiplication = in.readBoolean(); c.crossingTen = in.readBoolean();
                for (int i = 1; i <= 10; i++) c.tables[i] = in.readBoolean();
                c.validate(); generator.config = c;
                List<Integer> enabled = new ArrayList<>();
                if (c.addition) enabled.add(0);
                if (c.subtraction) enabled.add(1);
                if (c.multiplication) enabled.add(2);
                generator.families = readBag(in, enabled);
                for (int family = 0; family < 3; family++)
                    generator.candidates[family] = readBag(in, enabled.contains(family) ? pool(c, family) : null);
            }
            if (in.readBoolean()) {
                generator.last = new Exercise(in.readLong(), in.readInt(), in.readInt(), in.readUTF());
                if (generator.last.id != generator.nextId - 1 || generator.config == null)
                    throw new IllegalArgumentException("Ongeldig vorig somnummer.");
            } else if (generator.nextId != 1 || generator.config != null)
                throw new IllegalArgumentException("Vorige som ontbreekt.");
            if (in.available() != 0) throw new IllegalArgumentException("Onverwachte somgegevens.");
            return generator;
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException("Ongeldige somopslag.", e);
        }
    }

    private static void writeBag(DataOutputStream out, Bag bag) throws IOException {
        out.writeBoolean(bag != null);
        if (bag != null) { out.writeLong(bag.seed); out.writeInt(bag.cursor); out.writeInt(bag.offset); }
    }

    private static Bag readBag(DataInputStream in, List<Integer> source) throws IOException {
        if (!in.readBoolean()) {
            if (source != null) throw new IllegalArgumentException("Somzak ontbreekt.");
            return null;
        }
        if (source == null) throw new IllegalArgumentException("Onverwachte somzak.");
        Bag bag = new Bag(source, in.readLong(), -1);
        bag.cursor = in.readInt(); bag.offset = in.readInt();
        if (bag.cursor < 0 || bag.cursor > source.size() || bag.offset < 0 || bag.offset >= source.size())
            throw new IllegalArgumentException("Ongeldige plaats in somzak.");
        return bag;
    }

    private static boolean sameConfig(MathConfig a, MathConfig b) {
        if (a.limit != b.limit || a.addition != b.addition || a.subtraction != b.subtraction
                || a.multiplication != b.multiplication || a.crossingTen != b.crossingTen) return false;
        for (int i = 1; i <= 10; i++) if (a.tables[i] != b.tables[i]) return false;
        return true;
    }

    private static final class Bag {
        final List<Integer> source;
        final List<Integer> order;
        final long seed;
        int cursor;
        int offset;

        Bag(List<Integer> source, long seed, int previous) {
            this.source = source; this.seed = seed;
            order = new ArrayList<>(source);
            Collections.shuffle(order, new Random(seed));
            // A rotation preserves every candidate exactly once without persisting the whole bag.
            if (order.size() > 1 && order.get(0) == previous) offset = 1;
        }

        boolean exhausted() { return cursor == order.size(); }
        int take() { return order.get((offset + cursor++) % order.size()); }
    }
}
