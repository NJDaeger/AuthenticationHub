const regexExp = /[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/;
const uuidInput = document.getElementById("uuid");
const authInput = document.getElementById("auth-code");
const authorizeBtn = document.getElementById("authorize");

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

//Intializing toasts
var toastElList = [].slice.call(document.querySelectorAll('.toast'))
var toastList = toastElList.map(function (toastEl) {
    return new bootstrap.Toast(toastEl, {animation:true, autohide: true, delay: 5000});
})

//Adding event listeners to uuid and auth code input so nothing can be typed when disabled.
uuidInput.addEventListener("keydown", noType);
authInput.addEventListener("keydown", noType);

// if (window.location.href.contains("?session=")) {
//     //TODO: if it contains the session query param, we want to fetch the connections for this user.
// } else {
//     //TODO: if it does not contain the ?session= query param, load the input boxes
// }

var authForm = document.getElementById("authform");
var appList = document.getElementById("app-list");
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
})

//Disable typing in an input box
function noType(event) {
    if (!event.currentTarget.classList.contains("disabled")) return;
    event.stopPropagation();
    event.preventDefault();
    event.cancelBubble = true;
}

//Show a ripple effect
function startRipple(event) {
    const elem = event.currentTarget;
    if (elem.querySelector(".disabled")) return;
    const circle = document.createElement("span");
    const diameter = Math.max(elem.clientWidth, elem.clientHeight);
    const radius = diameter/2;

    circle.style.width = circle.style.height = `${diameter}px`;
    circle.style.left = `${event.clientX - (elem.offsetLeft + radius)}px`;
    circle.style.top = `${event.clientY - (elem.offsetTop + radius)}px`;
    circle.classList.add("ripple-effect");
    const ripple = elem.getElementsByClassName("ripple-effect")[0];
    if (ripple) ripple.remove();
    elem.appendChild(circle);
}

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

//TODO: make an endpoint for the connection buttons that gets called to for the user that checks the status of their auth session. If their auth session is good, forward them to the link


function hideForm() {
    authForm.classList.add("hiding");
    authForm.classList.remove("open");
}

function showForm() {
    authForm.classList.add("open");
    authForm.classList.remove("gone");
}

function hideAppList() {
    appList.classList.add("hiding");
    appList.classList.remove("open");
}

function showAppList() {
    appList.classList.add("open");
    appList.classList.remove("gone");
}

//
//Whether to show the loading bar on the page or not
//
function setLoading(loading) {
    if (loading) document.getElementsByClassName("loadbar")[0].classList.add("loading");
    else document.getElementsByClassName("loadbar")[0].classList.remove("loading");
}

//
//When the button is pressed, we need to do some checks for the frontend before we call the backend
//
function buttonPress() {
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
    }).then(res => {
        if (res.call) return {status: 200, message: "Authorized!", call: res.call};
        else return res.json()
    }).then(res => {
        const success = res.status === 200;
        if (res.call) {
            hideForm();
            connections(res.call);
        }
        if (success) {
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
        generateConnectionButtons(res.apps);
        showAppList();
        console.log(res);
        setLoading(false);
    }).catch(e => {
        console.log(e);
        setAuthButtonText("Validate UUID");
        showForm();
        hideAppList();
        setDisabled(authInput, true);
        authInput.value = null;
        setDisabled(uuidInput, false);
        setLoading(false);
        toast("API Error. Unable to authorize profiles right now, please try again later.", "bg-danger");
    });
}

function generateConnectionButtons(connections) {
    connections.forEach(elem => {
        var appContainerDiv = document.createElement('div');
        appContainerDiv.classList.add("col-xxl-4", "col-10", "m-2", "prime-button", "ripple");
        var appButton = document.createElement('button');
        appButton.id = elem.name;
        if (elem.connected) appButton.innerHTML = elem.name + `<span><i class="bi bi-check2"></i></span>`;
        else  appButton.innerHTML = elem.name;
        appContainerDiv.append(appButton);
        appList.append(appContainerDiv);
    });
}

/*
When the UUID is authorized, fade out the authform and start the loading bar until the response from the connections request is recieved. dont redirect the page
*/