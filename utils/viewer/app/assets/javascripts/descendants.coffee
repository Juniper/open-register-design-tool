# descendants.coffee - calls getDescendants with input id 
#                      maxdepth computed from current element depth and relative max
#                      uses returned json to generate descendants table
#
$ ->
  # ----- functions

  # create abbreviated category string
  createCatAbrString = (cat) ->
    result = ''
    if cat & 1 then result += ' SCFG' 
    if cat & 2 then result += ' DCFG' 
    if cat & 4 then result += ' CCFG' 
    if cat & 8 then result += ' SCTR' 
    if cat & 16 then result += ' ECTR' 
    if cat & 32 then result += ' ST' 
    if cat & 64 then result += ' INTR' 
    if cat & 128 then result += ' DIAG' 
    if cat & 256 then result += ' DBG' 
    if cat & 512 then result += ' CGU' 
    result
  
  # ----- create descendants table 
  decIdElems = $("#descendants")
  id = decIdElems.first().data('id')
  cdepth = decIdElems.first().data('cdepth')
  mdepth = decIdElems.first().data('mdepth')
  #alert("cdep=" + cdepth + ", cdepth=" + cdepth + ", mdepth=" + mdepth)
  rr = jsRoutes.controllers.Application.getDescendants(id, cdepth, mdepth)
  #alert("type=" + rr.type + ", url=" + rr.url)
  $.get rr.url, (registers) ->
    # ---- update number of results returned
    rescountEl = $("#rescount").first()
    rescountEl.text(registers.length)
    #alert("results=" + registers.length)
    # ---- build the table 
    hrow = $("<tr>")
    hrow.addClass("active")
    hrow.append $("<th></th>")
    hrow.append $("<th>name</th>")
    if id == 0 then hrow.append $("<th>label</th>")
    hrow.append $("<th>base address (0x)</th>")
    hrow.append $("<th>width</th>")
    hrow.append $("<th>access</th>")
    hrow.append $("<th>hier</th>")
    hrow.append $("<th>reps</th>")
    hrow.append $("<th>stride (0x)</th>")
    hrow.append $("<th>dt</th>")
    hrow.append $("<th>cat</th>")
    hrow.append $("<th>short text</th>")
    hrow.append $("<th>data id</th>")
#    hrow.append $("<th>depth</th>")
    hdr = $("<thead>").append hrow
    decIdElems.append hdr   
    bod = $("<tbody>")
    decIdElems.append bod   
    $.each registers, (index, register) ->
      brow = $("<tr>")
      if register.isRegset then brow.addClass("info")
      txt = if register.isRegset then '&#9776;' else ""
      brow.append $("<td>#{txt}</td>")
      indent = Array(register.depth+1).join '&nbsp;&nbsp;'
      if register.isRegset
         rr = jsRoutes.controllers.Application.regsetPage(register.id, register.depth, mdepth)
      else
         rr = jsRoutes.controllers.Application.registerPage(register.id)
      #alert("type=" + rr.type + ", url=" + rr.url)
      brow.append $("<td>#{indent}<a href=\"#{rr.url}\">#{register.name}</a></td>")
      if id == 0 then brow.append $("<td>#{register.parentPath}</td>")
      brow.append $("<td>#{register.baseAddr.toString(16)}</td>")
      txt = if register.isRegset then "" else register.width
      brow.append $("<td>#{txt}</td>")
      txt = if register.isRegset then "" else register.access
      brow.append $("<td>#{txt}</td>")
      txt = if register.isMap then 'map' else if register.extRoot then 'ext' else ''
      brow.append $("<td>#{txt}</td>") 
      brow.append $("<td>#{register.reps}</td>")
      txt = if register.reps > 1 then register.stride.toString(16) else ""
      brow.append $("<td>#{txt}</td>") 
      txt = if register.donttest then 'y' else ''
      brow.append $("<td>#{txt}</td>") 
      brow.append $("<td>#{createCatAbrString(register.catCode)}</td>")
      txt = register.shortText ? ""
      brow.append $("<td>#{txt}</td>")
      brow.append $("<td>#{register.loadId}</td>") 
#      brow.append $("<td>#{register.depth}</td>") 
      bod.append brow    
