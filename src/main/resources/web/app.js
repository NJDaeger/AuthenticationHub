const regexExp = /[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/;
const uuidInput = document.getElementById("uuid");
const authInput = document.getElementById("auth-code");
const authorizeBtn = document.getElementById("authorize");
const authForm = document.getElementById("authform");
const appList = document.getElementById("app-list");

//#region Ripple effect functions and logic

//
//First, I gotta do some basic website setup.
//* Ripple effect initialization
//* Toast initialization
//* Disable input initialization

//Adding all event listeners for the ripple effect to work
const ripples = document.getElementsByClassName("ripple");
for (const elem of ripples) {
    elem.addEventListener("click", startRipple);
}

//Show a ripple effect
function startRipple(event) {
    const elem = event.currentTarget;
    if (elem.querySelector(".disabled")) return;
    var rect = elem.getBoundingClientRect();
    const circle = document.createElement("span");
    const diameter = Math.max(elem.clientWidth, elem.clientHeight);
    const radius = diameter/2;

    circle.style.width = circle.style.height = `${diameter}px`;
    circle.style.left = `${event.clientX - (rect.left + radius)}px`;
    circle.style.top = `${event.clientY - (rect.top + radius)}px`;
    circle.classList.add("ripple-effect");
    const ripple = elem.getElementsByClassName("ripple-effect")[0];
    if (ripple) ripple.remove();
    elem.appendChild(circle);
}

//#endregion

//#region Toast effect functions and logic

//Intializing toasts
var toastElList = [].slice.call(document.querySelectorAll('.toast'))
var toastList = toastElList.map(function (toastEl) {
    return new bootstrap.Toast(toastEl, {animation:true, autohide: true, delay: 5000});
});

//
//Dynamically create a bootstrap toast and remove it after.
//
function toast(message, colorClass) {
    const container = document.getElementById("toast-container");
    const toastElem = document.createElement("div");
    toastElem.classList.add("toast", "hide", "align-items-center", "text-white", "border-0", colorClass);
    toastElem.setAttribute("role", "alert");;
    toastElem.ariaLive = "assertive";
    toastElem.ariaAtomic = "true";

    const contentContainer = document.createElement("div");
    contentContainer.classList.add("d-flex");

    const content = document.createElement("div");
    content.classList.add("toast-body");
    content.innerText = message;

    const closeBtn = document.createElement("button");
    closeBtn.type = "button";
    closeBtn.setAttribute("data-bs-dismiss", "toast");
    closeBtn.ariaLabel = "Close"
    closeBtn.classList.add("btn-close", "btn-close-white", "me-2", "m-auto");


    contentContainer.appendChild(content);
    contentContainer.appendChild(closeBtn);
    toastElem.appendChild(contentContainer);
    toastElem.addEventListener("hidden.bs.toast", () => {
        toastElem.remove();
    });
    container.appendChild(toastElem);
    new bootstrap.Toast(toastElem, {animation:true, autohide: true, delay: 5000}).show();
}

//#endregion

//#region Other page logic and functions

authForm.addEventListener('animationend', () => {
    if (authForm.classList.contains("hiding")) {
        authForm.classList.add("gone")
        authForm.classList.remove("hiding");
    }
});
appList.addEventListener('animationend', () => {
    if (appList.classList.contains("hiding")) {
        appList.classList.add("gone")
        appList.classList.remove("hiding");
    }
});

setLoading(true);
fetch("/info").then(res => res.json()).then(res => {
    document.getElementById("server-ip").innerHTML = res.auth_server_ip;
    const params = new URLSearchParams(window.location.search);
    if (params.has("state")) {
        connections("/applications?state=" + params.get("state"));
    } else showForm();
    setLoading(false);
}).catch(e => {
    console.log(e);
    showForm();
    setAuthButtonText("Validate UUID");
    setDisabled(authInput, true);
    authInput.value = null;
    setDisabled(uuidInput, false);
    setLoading(false);
    toast("Server Error. There was a problem requesting page info. Please try again later.", "bg-danger");
});

//
//This may seem kind of weird, however, when setting the element to disabled, we are unable to determine if
//said element is valid anymore. To get around that, we just disable it with a class and manually stop typing
//in the input box.
//
function setDisabled(elem, disabled) {
    if (disabled) elem.classList.add("disabled");
    else elem.classList.remove("disabled");
}

//
//Whether to show the loading bar on the page or not
//
function setLoading(loading) {
    if (loading) document.getElementsByClassName("loadbar")[0].classList.add("loading");
    else document.getElementsByClassName("loadbar")[0].classList.remove("loading");
}

//#endregion

//#region Auth form helper logic and functions

//
//When the button is pressed, we need to do some checks for the frontend before we call the backend
//
function authButton() {
    var id = document.getElementById('uuid').value;
    if (!regexExp.test(id)) {
        toast("UUID Error: Your UUID provided is not properly formatted.", "bg-danger");
        setAuthButtonText("Validate UUID");
        setDisabled(authInput, true);
        authInput.value = null;
        setDisabled(uuidInput, false);
        setLoading(false);
        return;
    } else if (authInput.classList.contains("disabled")) validate();
    else authorize();
    setLoading(true);
}

//
//Sets the button text
//
function setAuthButtonText(text) {
    const authParent = authorizeBtn.parentElement;
    authParent.classList.add("hide-text");
    setTimeout(() => {
        authorizeBtn.innerText = text;
        authParent.classList.remove("hide-text");
    }, 100);
}

//
//Hides the auth form
//
function hideForm() {
    if (authForm.classList.contains("gone")) return;
    authForm.classList.add("hiding");
    authForm.classList.remove("open");
}

//
//Shows the auth form
//
function showForm() {
    if (authForm.classList.contains("open")) return;
    authForm.classList.add("open");
    authForm.classList.remove("gone");
}

//Adding event listeners to uuid and auth code input so nothing can be typed when disabled.
uuidInput.addEventListener("keydown", noType);
authInput.addEventListener("keydown", noType);

//Disable typing in an input box
function noType(event) {
    if (!event.currentTarget.classList.contains("disabled")) return;
    event.stopPropagation();
    event.preventDefault();
    event.cancelBubble = true;
}

//#endregion

//#region API request functions

//
//Validate the user UUID
//
function validate() {
    fetch("/validate", {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({uuid: uuidInput.value})
    }).then(res => res.json()).then(res => {
        const success = res.status === 200;
        if (success) {
            setAuthButtonText("Authorize");
            setDisabled(uuidInput, true);
            setDisabled(authInput, false);
            setLoading(false);
        } else {
            setAuthButtonText("Validate UUID");
            setDisabled(authInput, true);
            authInput.value = null;
            setDisabled(uuidInput, false);
            setLoading(false);
        }
        toast(res.message, success ? "bg-success" : "bg-danger");
    }).catch(e => {
        console.log(e);
        setAuthButtonText("Validate UUID");
        setDisabled(authInput, true);
        authInput.value = null;
        setDisabled(uuidInput, false);
        setLoading(false);
        toast("API Error. Unable to authorize profiles right now, please try again later.", "bg-danger");
    });
}

//
//Authorize this user
//
function authorize() {
    fetch("/authorize", {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({authCode: authInput.value, uuid: uuidInput.value})
    }).then(res => res.json()).then(res => {
        const success = res.status === 200;
        if (res.call) {
            connections(res.call);
        }
        else if (success) {
            setAuthButtonText("Authorized!");
            setDisabled(uuidInput, true);
            setDisabled(authInput, true);
            setLoading(false);
        } else {
            setAuthButtonText("Validate UUID");
            setDisabled(authInput, true);
            authInput.value = null;
            setDisabled(uuidInput, false);
            setLoading(false);
        }
        toast(res.message, success ? "bg-success" : "bg-danger");
    }).catch(e => {
        console.log(e);
        setAuthButtonText("Validate UUID");
        setDisabled(authInput, true);
        authInput.value = null;
        setDisabled(uuidInput, false);
        setLoading(false);
        toast("API Error. Unable to authorize profiles right now, please try again later.", "bg-danger");
    });
}

function connections(url) {
    setLoading(true);
    fetch(url).then(res => res.json()).then(res => {
        const success = res.status === 200;
        console.log(res);
        if (success) {
            hideForm();
            generateConnectionButtons(res.apps);
            showAppList();
        } else {
            hideAppList();
            showForm();
            toast(res.message, "bg-danger");
        }
        setLoading(false);
    }).catch(e => {
        console.log(e);
        window.history.pushState({}, document.title, window.location.pathname);
        // setAuthButtonText("Validate UUID");
        // hideAppList();
        // showForm();
        // setDisabled(authInput, true);
        // authInput.value = null;
        // setDisabled(uuidInput, false);
        setLoading(false);
        // toast("API Error. Unable to authorize profiles right now, please try again later.", "bg-danger");
    });
}

//#endregion

//#region App list helper functions
function generateConnectionButtons(connections) {
    connections.forEach(connection => {
        var appContainerDiv = document.createElement('div');
        appContainerDiv.classList.add("col-xxl-4", "col-10", "m-2", "prime-button", "ripple");
        var appButton = document.createElement('button');
        appButton.id = connection.name;
        //if the connection property is null, that means the user has already connected to that service.
        if (!connection.connection) appButton.innerHTML = connection.name + `<span><i class="bi bi-check2"></i></span>`;
        else  appButton.innerHTML = connection.name;
        appContainerDiv.append(appButton);
        appList.append(appContainerDiv);
    });
}

function hideAppList() {
    if (appList.classList.contains("gone")) return;
    appList.classList.add("hiding");
    appList.classList.remove("open");
}

function showAppList() {
    if (appList.classList.contains("open")) return;
    appList.classList.add("open");
    appList.classList.remove("gone");
}

//#endregion