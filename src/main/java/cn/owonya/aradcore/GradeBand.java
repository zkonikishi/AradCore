package cn.owonya.aradcore;

record GradeBand(int min, int max, int weight) {
    int size() { return max - min + 1; }
}
