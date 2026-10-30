// Asks for confirmation, then submits the hidden POST form from delete-form.html
function deleteStudent(link) {
    if (!confirm('Delete this student?')) {
        return false;
    }
    var form = document.getElementById('deleteForm');
    form.querySelector('input[name="id"]').value = link.getAttribute('data-id');
    form.submit();
    return false;
}
