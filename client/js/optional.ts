import {isDefined, isNullOrUndefined} from "./preconditions";


export interface Optional<T extends NonNullable<any>> {
    readonly value: T | never

    isPresent(): boolean

    map<S>(mapper: (from: T) => S): Optional<NonNullable<S>>

    flatMap<S>(mapper: (from: T) => Optional<S>): Optional<S>

    filter(filter: (from: T) => boolean): Optional<T>

    orElse<U>(other: U): T | U

    orMaybe<U>(other: U | null | undefined): Optional<T | U>
}

export interface PresentOptional<T extends NonNullable<any>> extends Optional<T> {
    readonly value: T

    isPresent(): true

    orElse<U>(other: U): T

    orMaybe<U>(other: U | null | undefined): PresentOptional<T>

}

export interface AbsentOptional<T extends NonNullable<any>> extends Optional<T> {
    readonly value: never

    isPresent(): false

    map<S>(mapper: (from: T) => S): AbsentOptional<NonNullable<S>>

    flatMap<S>(mapper: (from: T) => Optional<S>): AbsentOptional<S>

    filter(filter: (from: T) => boolean): AbsentOptional<T>

    orElse<U>(other: U): U

    orMaybe<U>(other: U | null | undefined): Optional<U>
}

class OptionalImpl<T> implements PresentOptional<T> {
    constructor(public value: T) {
    }

    isPresent(): true {
        return true;
    }

    map<S>(mapper: (from: T) => S) {
        const value = mapper(this.value);
        if (isNullOrUndefined(value)) {
            return emptyOptional<S>();
        }
        return optional(value);
    }

    flatMap<S>(mapper: (from: T) => Optional<S>) {
        return mapper(this.value) as any;
    }

    filter(filter: (from: T) => boolean): Optional<T> {
        if (!filter(this.value)) {
            return emptyOptional<T>();
        }
        return this;
    }

    orElse<U>(other: U): T {
        return this.value;
    }

    orMaybe<U>(other: U | undefined | null): PresentOptional<T> {
        return this;
    }

}

const EMPTY_OPTIONAL: AbsentOptional<any> = {
    get value(): never {
        throw Error('Empty Optional!');
    },
    isPresent(): false {
        return false;
    },
    map<S>(): AbsentOptional<S> {
        return emptyOptional<S>();
    },
    flatMap<S>(): AbsentOptional<S> {
        return emptyOptional<S>();
    },
    filter() {
        return this;
    },
    orElse<U>(other: U): U {
        return other;
    },
    orMaybe<U>(other: U | undefined | null): Optional<U> {
        return optional(other);
    }
};

export function emptyOptional<T>(): AbsentOptional<NonNullable<T>> {
    return EMPTY_OPTIONAL;
}

export function optional<T>(value: T): Optional<NonNullable<T>> {
    if (isDefined(value)) {
        return new OptionalImpl(value);
    }
    return emptyOptional<T>();
}
