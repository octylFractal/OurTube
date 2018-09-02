import $ from "jquery";
import {checkNotNull} from "./preconditions";


type HTMLElementWithValidity = HTMLElement & {
    checkValidity(): boolean
}

const INVALIDITY_CLASS = 'has-danger';

export function hookFields(invalidityClass: string = INVALIDITY_CLASS) {
    $("body").on('change', e => {
        const anyTarget = e.target as any;
        if (typeof anyTarget.checkValidity === "undefined") {
            return;
        }
        const target = anyTarget as HTMLElementWithValidity;
        if (target.checkValidity()) {
            checkNotNull(target.parentElement).classList.remove(invalidityClass)
        }
    });
    const obs = new MutationObserver((records: MutationRecord[]) => {
        for (let rec of records) {
            const forms = $(rec.addedNodes).find('form');
            forms.on('submit', e => {
                $(e.target).find(`.${invalidityClass}`).removeClass(invalidityClass);
            });
            forms.find('input').on('invalid', e => {
                e.currentTarget.forEach(node => {
                    checkNotNull(node.parentElement).classList.add(invalidityClass);
                });
            });
        }
    });
    obs.observe(document.body, {
        childList: true,
        subtree: true
    });
}