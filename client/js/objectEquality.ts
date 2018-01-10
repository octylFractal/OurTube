/**
 * Simple comparison function that looks at enumerable fields for objects and identities for non-objects.
 */
export function valuesEqual(value1: any, value2: any): boolean {
    if (typeof value1 !== 'object' || value1 === null) {
        return value1 === value2;
    }
    // try array fast comparison
    if (value1 instanceof Array) {
        return value2 instanceof Array && arraysEqual(value1, value2);
    }
    const keySet = new Set<string>();
    Object.keys(value1).forEach(k => keySet.add(k));
    for (let k of Object.keys(value2)) {
        if (!keySet.has(k)) {
            return false;
        }
        if (!valuesEqual(value1[k], value2[k])) {
            return false;
        }
    }
    return true;
}
function arraysEqual(value1: any[], value2: any[]): boolean {
    if (value1.length !== value2.length) {
        return false;
    }
    for (let i = 0; i < value1.length; i++) {
        if (!valuesEqual(value1[i], value2[i])) {
            return false;
        }
    }
    return true;
}
