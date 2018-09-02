import "./shims";
import $ from "jquery";
import {createNavbar} from "./navbar";
import ReactDOM from "react-dom";
import {Route, Router} from "./router/router";
import React from "react";
import {mainPage} from "./routes/mainPage";
import {hookFields} from "./invalidateForms";
import {Provider} from "react-redux";
import {ISTATE} from "./reduxish/store";
import queryString from "query-string";
import {discordLogin} from "./discordLoginImport";
import {discordInformationFromLocalStorage} from "./reduxish/discordLocalStorage";

function getRoutes() {
    const routes = new Map<string, Route>();
    routes.set('', mainPage);
    return routes;
}

function discordRedirect() {
    const discRed = queryString.parse(window.location.search)['discordRedirect'];
    return discRed === 'true';
}

discordInformationFromLocalStorage();

$(() => {
    const navbar = createNavbar();
    const routes = getRoutes();
    ReactDOM.render(
        <div>
            <Provider store={ISTATE}>
                <div>
                    {navbar}
                    {
                        discordRedirect()
                            ? null
                            : <Router paths={routes}/>
                    }
                </div>
            </Provider>
        </div>,
        document.getElementById("container"),
        () => {
            hookFields();
            if (discordRedirect()) {
                // store hash data into local storage
                const hashData = queryString.parse(window.location.hash);
                discordLogin(hashData);
                window.location.replace('/');
            }
        });
});
