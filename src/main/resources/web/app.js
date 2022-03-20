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

function showToast(toastId) {
    toastList.find(toast => toast._element.id === toastId)?.show();
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

//
//Whether to show the loading bar on the page or not
//
function setLoading(loading) {
    if (loading) document.getElementsByClassName("loadbar")[0].classList.add("loading");
    else document.getElementsByClassName("loadbar")[0].classList.remove("loading");
}

//
//Authorize/validate the user when input is given.
//
async function authorize() {
    var id = document.getElementById('uuid').value;
    if (!regexExp.test(id)) {
        showToast("toast-id-bad");
        setAuthButtonText("Validate UUID");
        setDisabled(authInput, true);
        authInput.value = null;
        setDisabled(uuidInput, false);
        setLoading(false);
        return;
    }
    else {
        fetch("http://localhost:4567/validate?uuid=" + id).then(res => res.json()).then(res => {
            if (res.error) {
                showToast("toast-id-missing");
                setAuthButtonText("Validate UUID");
                setDisabled(authInput, true);
                authInput.value = null;
                setDisabled(uuidInput, false);
                setLoading(false);
            } else {
                showToast("toast-id-valid");
                setAuthButtonText("Authorize");
                setDisabled(uuidInput, true);
                setDisabled(authInput, false);
                setLoading(false);
            }
        }).catch(e => {
            showToast("toast-api-error");
            setAuthButtonText("Validate UUID");
            setDisabled(authInput, true);
            authInput.value = null;
            setDisabled(uuidInput, false);
            setLoading(false);
        });
    }
    setLoading(true);
}