# loads.coffee - calls getLoads and
#                      uses returned json to generate loads table
#
$ ->
  forceTwoDigits = (val) ->
    if val < 10
      return "0#{val}"
    return val
    
  ownerPdClick = (name) ->
    #alert("pulldown click, name=" + name)
    $("#fowner").val(name)
    $('#fowner_form').submit()

  getOwnerLi = (name) ->
    fowner_li = $("<li>" + name + "</li>")
    fowner_li.click ->
      ownerPdClick(name) 
      
  loadIdElems = $("#loads")
  targetOwner = loadIdElems.data('fowner')
  targetOwner ?= 'any'
  rr = jsRoutes.controllers.Application.getLoads()
  #alert("type=" + rr.type + ", url=" + rr.url)
  $.get rr.url, (loads) ->
    hrow = $("<tr>")
    hrow.addClass("active")
    hrow.append $("<th>id</th>")
    # --- add the owner pulldown in header cell
    ownerhd = $("<th>")
    hrow.append ownerhd
    rr = jsRoutes.controllers.Application.loadPage()
    #alert("type=" + rr.type + ", url=" + rr.url)
    pform = $('<form role="form" name="fowner_form" id="fowner_form" method="GET" enctype="application/x-www-form-urlencoded" action="' + rr.url + '"/>')
    pform.append $('<input type="hidden" name="fileOwner" id="fowner"/>')
    pform_div = $('<div class="dropdown"/>')
    pform.append pform_div
    pform_div.append $('<button class="btn btn-primary dropdown-toggle" type="button" data-toggle="dropdown">owner<span class="caret"></span></button>')
    fowner_pd = $('<ul class="dropdown-menu"/>')
    pform_div.append fowner_pd
    fowner_pd.append getOwnerLi('any')
    ownerhd.append pform
    hrow.append $("<th>timestamp</th>")
    hrow.append $("<th>label</th>")
    hrow.append $('<th class="wrappable">file</th>')
    hrow.append $("<th>reload</th>")
    hrow.append $("<th>delete</th>")
    hdr = $("<thead>").append hrow
    loadIdElems.append hdr   
    bod = $("<tbody>")
    loadIdElems.append bod
    uniqueOwners = []   
    $.each loads, (index, load) ->
      #alert("file=" + load.file + ", label=" + load.label + ", id=" + load.id)
      brow = $("<tr>")
      rr = jsRoutes.controllers.Application.regsetPage(load.rootRegId, 0, 1)
      #alert("type=" + rr.type + ", url=" + rr.url)
      # --- add uniqueOwnwers to dropdown list
      if load.user not in uniqueOwners
        fowner_pd.append getOwnerLi(load.user)
        uniqueOwners.push load.user
      # --- if file owner selected
      if (targetOwner == 'any') or (targetOwner == '') or (load.user == targetOwner)
        brow.append $("<td><a href=\"#{rr.url}\">#{load.id}</a></td>")
        brow.append $("<td>#{load.user}</td>")
        dval = new Date(load.timestamp)
        dstr = (dval.getMonth()+1) + '/' + dval.getDate() + '/' + dval.getFullYear() + ' ' + dval.getHours() + ':' + forceTwoDigits(dval.getMinutes())
        brow.append $("<td>#{dstr}</td>")
        brow.append $("<td>#{load.label}</td>")
        brow.append $('<td class="wrappable">' + load.file + '</td>')
        # --- add the reload button for this row
        rr = jsRoutes.controllers.Application.loadFile()
        bform = $('<form role="form" method="POST" enctype="application/x-www-form-urlencoded" action="' + rr.url + '"/>')
        bform.append $('<input type="hidden" name="fname" value="' + load.file + '"/>')
        bform.append $('<input type="hidden" name="flabel" value="' + load.label + '"/>')
        bform.append $('<input type="hidden" name="freplace" value="1"/>')
        bform.append $('<input type="hidden" name="replaceid" value="' + load.id + '"/>')
        bform.append $('<input type="hidden" name="fowner" value="' + targetOwner + '"/>')
        bform.append $('<button type="submit" class="btn btn-info">&#8635;</button>')
        belem = $("<td/>")
        belem.append bform
        brow.append belem
        # --- add the delete button for this row
        rr = jsRoutes.controllers.Application.delete()
        bform = $('<form role="form" method="POST" enctype="application/x-www-form-urlencoded" action="' + rr.url + '"/>')
        bform.append $('<input type="hidden" name="id" value="' + load.id + '"/>')
        bform.append $('<input type="hidden" name="fowner" value="' + targetOwner + '"/>')
        bform.append $('<button type="submit" class="btn btn-danger">&#9747;</button>')
        belem = $("<td/>")
        belem.append bform
        brow.append belem
        bod.append brow