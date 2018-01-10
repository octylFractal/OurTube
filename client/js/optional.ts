type Maybe<S> = S | undefined;

export interface Optional<T> {
    readonly value: T

    isPresent(): boolean

    map<S>(mapper: (from: T) => Maybe<S>): Optional<S>

    flatMap<S>(mapper: (from: T) => Optional<S>): Optional<S>

    filter(filter: (from: T) => boolean): Optional<T>

    orElse(other: T): T

    orElseUndefined(): T | undefined
}

class OptionalImpl<T> implements Optional<T> {
    constructor(public value: T) {
    }

    isPresent() {
        return true;
    }

    map<S>(mapper: (from: T) => Maybe<S>) {
        return optional(mapper(this.value));
    }

    flatMap<S>(mapper: (from: T) => Optional<S>) {
        return mapper(this.value);
    }

    filter(filter: (from: T) => boolean): Optional<T> {
        if (!filter(this.value)) {
            return emptyOptional();
        }
        return this;
    }

    orElse(other: T): T {
        return this.value;
    }

    orElseUndefined(): T {
        return this.value;
    }
}

const EMPTY_OPTIONAL: Optional<any> = {
    get value() {
        throw Error('Empty Optional!');
    },
    isPresent() {
        return false;
    },
    map<S>(): Optional<S> {
        return emptyOptional();
    },
    flatMap<S>(): Optional<S> {
        return emptyOptional();
    },
    filter() {
        return this;
    },
    orElse(other: any): any {
        return other;
    },
    orElseUndefined(): undefined {
        return undefined;
    }
};

export function emptyOptional(): Optional<any> {
    return EMPTY_OPTIONAL;
}

export function optional<T>(value: T | undefined): Optional<T> {
    return typeof value === "undefined" ? emptyOptional() : new OptionalImpl(value);
}
