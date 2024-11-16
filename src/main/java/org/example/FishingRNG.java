import com.google.common.hash.Hashing;
import com.google.common.primitives.Longs;

import java.nio.charset.StandardCharsets;

public class FishingRNG {
    private long seedLo;
    private long seedHi;
    private final long seedLoHash;
    private final long seedHiHash;

    public FishingRNG(String identifier) {
        byte[] hashBytes = Hashing.md5()
                .hashString(identifier, StandardCharsets.UTF_8)
                .asBytes();
        seedLoHash = Longs.fromBytes(hashBytes[0], hashBytes[1], hashBytes[2], hashBytes[3],
                hashBytes[4], hashBytes[5], hashBytes[6], hashBytes[7]);
        seedHiHash = Longs.fromBytes(hashBytes[8], hashBytes[9], hashBytes[10], hashBytes[11],
                hashBytes[12], hashBytes[13], hashBytes[14], hashBytes[15]);
    }

    public void setSeed(long seed) {
        long l2 = seed ^ 0x6A09E667F3BCC909L;
        long l3 = l2 + -7046029254386353131L;
        this.seedLo = mixStafford13(l2 ^ seedLoHash);
        this.seedHi = mixStafford13(l3 ^ seedHiHash);

        if ((this.seedLo | this.seedHi) == 0L) {
            this.seedLo = -7046029254386353131L;
            this.seedHi = 7640891576956012809L;
        }
    }

    public long nextLong() {
        long l = this.seedLo;
        long m = this.seedHi;
        long n = Long.rotateLeft(l + m, 17) + l;
        m ^= l;
        this.seedLo = Long.rotateLeft(l, 49) ^ m ^ (m << 21);
        this.seedHi = Long.rotateLeft(m, 28);
        return n;
    }

    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("Bound must be positive");
        }
        long next = Integer.toUnsignedLong((int) this.nextLong());
        return (int) (next * bound >> 32);
    }

    private long mixStafford13(long seed) {
        seed = (seed ^ (seed >>> 30)) * -4658895280553007687L;
        seed = (seed ^ (seed >>> 27)) * -7723592293110705685L;
        return seed ^ (seed >>> 31);
    }

    static class WeightedItem {
        String name;
        int weight;

        WeightedItem(String name, int weight) {
            this.name = name;
            this.weight = weight;
        }
    }

    public static void main(String[] args) {
        // Configurable parameters
        final long seedUpperLimit = 100000000; // Seedの上限
        final int totalAttempts = 12; // 総釣り回数
        final int requiredNautilusCount = 8; // 必要なオウムガイの殻の数

        // Loot table for treasure check
        WeightedItem[] treasureTable = {
                new WeightedItem("junk", 10),
                new WeightedItem("treasure", 5),
                new WeightedItem("fish", 85)
        };

        // Treasure pool for specific items
        WeightedItem[] treasurePool = {
                new WeightedItem("name_tag", 1),
                new WeightedItem("saddle", 1),
                new WeightedItem("bow", 1),
                new WeightedItem("fishing_rod", 1),
                new WeightedItem("book", 1),
                new WeightedItem("nautilus_shell", 1)
        };

        String lootIdentifier = "minecraft:gameplay/fishing";
        FishingRNG rng = new FishingRNG(lootIdentifier);

        System.out.printf("Searching for seeds (Upper Limit: %d, Nautilus Target: %d+/%d)...%n", seedUpperLimit, requiredNautilusCount, totalAttempts);

        for (long seed = 1; seed <= seedUpperLimit; seed++) {
            rng.setSeed(seed);
            if (hasNautilusInAttempts(rng, treasureTable, treasurePool, totalAttempts, requiredNautilusCount)) {
                System.out.println("Found seed: " + seed);
            }
        }
    }

    public static boolean hasNautilusInAttempts(FishingRNG rng, WeightedItem[] treasureTable, WeightedItem[] treasurePool, int totalAttempts, int requiredNautilusCount) {
        int nautilusCount = 0;

        for (int i = 0; i < totalAttempts; i++) {
            // 奇数回目: treasure判定
            String result = rollLoot(treasureTable, rng);
            if ("treasure".equals(result)) {
                // 偶数回目: treasure内アイテム判定
                String treasureItem = rollLoot(treasurePool, rng);
                if ("nautilus_shell".equals(treasureItem)) {
                    nautilusCount++;
                }
            } else {
                // Treasure以外でも乱数を消費
                rng.nextInt(1);
            }

            // オウムガイの殻が8個以上になったら成功
            if (nautilusCount >= requiredNautilusCount) {
                return true;
            }
        }

        // 最後まで8個に満たない場合は失敗
        return false;
    }

    public static String rollLoot(WeightedItem[] lootTable, FishingRNG rng) {
        int totalWeight = 0;
        for (WeightedItem item : lootTable) {
            totalWeight += item.weight;
        }

        int roll = rng.nextInt(totalWeight);
        int cumulativeWeight = 0;

        for (WeightedItem item : lootTable) {
            cumulativeWeight += item.weight;
            if (roll < cumulativeWeight) {
                return item.name;
            }
        }

        throw new IllegalStateException("Failed to select loot");
    }
}
