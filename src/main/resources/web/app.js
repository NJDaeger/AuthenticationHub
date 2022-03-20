const regexExp = /[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/;
const uuidInput = document.getElementById("uuid");
const authInput = document.getElementById("auth-code");
const authorizeBtn = document.getElementById("authorize");

function startRipple(event) {
    const elem = event.currentTarget;
    if (elem.querySelector(".disabled")) return;
    const circle = document.createElement("span");
    const diameter = Math.max(elem.clientWidth, elem.clientHeight);
    const radius = diameter/2;

    console.log(diameter);

    circle.style.width = circle.style.height = `${diameter}px`;
    circle.style.left = `${event.clientX - (elem.offsetLeft + radius)}px`;
    circle.style.top = `${event.clientY - (elem.offsetTop + radius)}px`;
    circle.classList.add("ripple-effect");
    const ripple = elem.getElementsByClassName("ripple-effect")[0];
    if (ripple) ripple.remove();
    elem.appendChild(circle);
}

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

function showToast(toastId) {
    toastList.find(toast => toast._element.id === toastId)?.show();
}

function setDisabled(elem, disabled) {
    if (disabled) {
        elem.disabled = true;
        elem.classList.add("disabled");
    } else {
        elem.disabled = false;
        elem.classList.remove("disabled");
    }
}

function setAuthButtonText(text) {
    authorizeBtn.innerText = text;
}

function setLoading(loading) {
    console.log("SET" + loading);
    if (loading) document.getElementsByClassName("loadbar")[0].classList.add("loading");
    else document.getElementsByClassName("loadbar")[0].classList.remove("loading");
}

async function authorize() {
    var id = document.getElementById('uuid').value;
    if (!regexExp.test(id)) {
        showToast("toast-id-bad");
        setAuthButtonText("Validate UUID");
        setDisabled(authInput, true);
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
                setDisabled(uuidInput, false);
                setLoading(false);
            } else {
                showToast("toast-id-valid");
                setAuthButtonText("Authorize");
                setDisabled(uuidInput, true);
                setDisabled(authInput, false);
                setLoading(false);

                // docu
                // document.getElementById('hello-message').innerHTML = "Hi " + res.name;
                // hide('uuid-not-found');
                // hide('uuid-invalid');
                // show('uuid-found');
            }
        }).catch(e => {
            showToast("toast-api-error");
            setAuthButtonText("Validate UUID");
            setDisabled(uuidInput, true);
                setDisabled(authInput, false);
                setLoading(false);
            // setDisabled(authInput, true);
            // setDisabled(uuidInput, false);
            // setLoading(false);
        });
    }
    setLoading(true);
}

// async function validateAccount() {
//     var id = document.getElementById('uuid').value;
//     if (!regexExp.test(id)) {
//         hide('uuid-not-found');
//         hide('uuid-found');
//         show('uuid-invalid');
//     } else {
//         hide('uuid-invalid');
//         await fetch("http://localhost:4567/validate?uuid=" + id).then(res => res.json()).then(res => {
//             if (res.error) {
//                 hide('uuid-found');
//                 hide('uuid-invalid');
//                 show('uuid-not-found');
//             } else {
//                 document.getElementById('hello-message').innerHTML = "Hi " + res.name;
//                 hide('uuid-not-found');
//                 hide('uuid-invalid');
//                 show('uuid-found');
//             }
//         }).catch(e => {
//             console.log(e);
//             // hide('loader');
//             hide('uuid-found');
//             hide('uuid-invalid');
//             show('uuid-not-found');
//         });
//         // show('loader');
//     }
// }