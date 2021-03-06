const regexExp = /^[a-zA-Z0-9_]{3,16}$/;
//regular expression to check if the username is valid

const usernameInput = document.getElementById("username");
const authInput = document.getElementById("auth-code");
const authorizeBtn = document.getElementById("authorize");
const authForm = document.getElementById("auth-form");
const appList = document.getElementById("app-list");

var serverInfo = { auth_server_ip: "[undisclosed]"};
var firstInstruction = `Please provide your Minecraft Java Edition Username.`;
var secondInstruction = () => `Next, join <code>${serverInfo.auth_server_ip}</code> and provide the authorization code given in the kick message, or by running <code>/authhub</code> in game.`;
var thirdInstruction = `Select the service below that you would like to connect!`;

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

function getIp() {
    return serverInfo.auth_server_ip;
}

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
    // document.getElementById("server-ip").innerHTML = res.auth_server_ip;
    serverInfo = res;
    const params = new URLSearchParams(window.location.search);
    if (params.has("state")) {
        connections("/applications?state=" + params.get("state"));
    } else showForm();
    setInstructionMessage(firstInstruction);
    setLoading(false);
}).catch(e => {
    showForm();
    setInstructionMessage(firstInstruction);
    setAuthButtonText("Validate Username");
    setDisabled(authInput, true);
    authInput.value = null;
    setDisabled(usernameInput, false);
    setLoading(false);
    toast("Server Error. There was a problem requesting page info. Please try again later.", "bg-danger");
});

//
//This may seem kind of weird, however, when setting the element to disabled, we are unable to determine if
//said element is valid anymore. To get around that, we just disable it with a class and manually stop typing
//in the input box.
//
function setDisabled(elem, disabled) {
    if (disabled) {
        elem.classList.add("disabled");
        elem.setAttribute("tabindex", "-1");
    } else {    
        elem.setAttribute("tabindex", "0");
        elem.focus();
        elem.classList.remove("disabled");
    }
}

//
//Whether to show the loading bar on the page or not
//
function setLoading(loading) {
    if (loading) document.getElementsByClassName("loadbar")[0].classList.add("loading");
    else document.getElementsByClassName("loadbar")[0].classList.remove("loading");
}

function setInstructionMessage(message) {
    const instructions = document.getElementById("instruction-message");
    if (message == null) {
        instructions.classList.remove("open");
        instructions.classList.add("hiding");
    } else if (instructions.classList.contains("hiding")) {
        instructions.innerHTML = message instanceof Function ? message() : message;
        instructions.classList.remove("hiding");
        instructions.classList.add("open");
    } else {
        instructions.classList.remove("open");
        instructions.classList.add("hiding");
        setTimeout(() => {
            instructions.innerHTML =  message instanceof Function ? message() : message;
            instructions.classList.add("open");
            instructions.classList.remove("hiding");
        }, 500);
    }
}

//#endregion

//#region Auth form helper logic and functions

//
//When the button is pressed, we need to do some checks for the frontend before we call the backend
//
function authButton() {
    var id = document.getElementById('username').value;
    if (!regexExp.test(id)) {
        toast("Username Error: Your username provided is not properly formatted.", "bg-danger");
        setAuthButtonText("Validate Username");
        setInstructionMessage(firstInstruction);
        setDisabled(authInput, true);
        authInput.value = null;
        setDisabled(usernameInput, false);
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
usernameInput.addEventListener("keydown", noType);
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
//Validate the username
//
function validate() {
    fetch("/validate", {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({username: usernameInput.value})
    }).then(res => res.json()).then(res => {
        const success = res.status === 200;
        if (success) {
            setAuthButtonText("Authorize");
            setDisabled(usernameInput, true);
            setInstructionMessage(secondInstruction);
            setDisabled(authInput, false);
            setLoading(false);
        } else {
            setAuthButtonText("Validate Username");
            setDisabled(authInput, true);
            setInstructionMessage(firstInstruction);
            authInput.value = null;
            setDisabled(usernameInput, false);
            setLoading(false);
        }
        toast(res.message, success ? "bg-success" : "bg-danger");
    }).catch(e => {
        console.log(e);
        setAuthButtonText("Validate Username");
        setDisabled(authInput, true);
        setInstructionMessage(firstInstruction);
        authInput.value = null;
        setDisabled(usernameInput, false);
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
        body: JSON.stringify({authCode: authInput.value, username: usernameInput.value})
    }).then(res => res.json()).then(res => {
        const success = res.status === 200;
        if (res.call) {
            connections(res.call);
        }
        else if (success) {
            setAuthButtonText("Authorized!");
            setDisabled(usernameInput, true);
            setDisabled(authInput, true);
            setInstructionMessage(thirdInstruction);
            setLoading(false);
        } else {
            setAuthButtonText("Validate Username");
            setDisabled(authInput, true);
            setInstructionMessage(firstInstruction);
            authInput.value = null;
            setDisabled(usernameInput, false);
            setLoading(false);
        }
        toast(res.message, success ? "bg-success" : "bg-danger");
    }).catch(e => {
        console.log(e);
        setAuthButtonText("Validate Username");
        setDisabled(authInput, true);
        setInstructionMessage(firstInstruction);
        authInput.value = null;
        setDisabled(usernameInput, false);
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
            setInstructionMessage(thirdInstruction);
            generateConnectionButtons(res.apps);
            showAppList();
        } else {
            hideAppList();
            showForm();
            toast(res.message, "bg-danger");
            window.history.pushState({}, document.title, window.location.pathname);
        }
        setLoading(false);
    }).catch(e => {
        window.history.pushState({}, document.title, window.location.pathname);
        setLoading(false);
        toast("API Error. Error fetching user connections. Please try again later.", "bg-danger");
    });
}

//#endregion

//#region App list helper functions
function generateConnectionButtons(connections) {
    connections.forEach(connection => {
        var appContainerDiv = document.createElement('div');
        appContainerDiv.classList.add("col-xxl-6", "my-2", "prime-button", "ripple");
        appContainerDiv.addEventListener("click", startRipple);
        if (!connection.connection) appContainerDiv.dataset.type = "connected";
        var appButton = document.createElement('a');
        appButton.id = connection.name;
        //if the connection property is null, that means the user has already connected to that service.
        if (!connection.connection) {
            appButton.innerHTML = connection.name + `<span><i class="bi bi-check2"></i></span>`;
        }
        else {
            appButton.innerHTML = connection.name;
            appButton.href = connection.connection;
        }
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